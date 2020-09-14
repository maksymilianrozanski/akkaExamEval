package exams.http

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import exams.data.StudentsExam
import exams.{ExamDistributor, RequestExam, Student}

object StudentActions {

  sealed trait Command
  final case class TestCommand(replyTo: ActorRef[ActionPerformed]) extends Command
  final case class RequestExamCommand(code: String, replyTo: ActorRef[ExamToDisplay], distributor: ActorRef[ExamDistributor]) extends Command

  final case class ActionPerformed(description: String)
  final case class ExamToDisplay(exam: StudentsExam)

  def apply(): Behavior[Command] = registry()

  def registry(): Behavior[Command] = {
    Behaviors.setup(context =>
      Behaviors.receiveMessage {
        case TestCommand(replyTo) =>
          context.log.info("received test command")
          replyTo ! ActionPerformed("test action performed!")
          Behaviors.same
        case RequestExamCommand(code, displayReceiver, distributor) =>
          context.log.info("received starting exam request")
          val student = context.spawnAnonymous(Student(displayReceiver))
          distributor ! RequestExam(student)
          Behaviors.same
      })
  }
}
