package exams.http

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import exams.ExamDistributor.{ExamDistributor, RequestExam}
import exams.Student
import exams.data.StudentsExam

object StudentActions {

  sealed trait Command
  final case class RequestExamCommand(code: String, replyTo: ActorRef[ExamToDisplay], distributor: ActorRef[ExamDistributor]) extends Command

  final case class ActionPerformed(description: String)
  final case class ExamToDisplay(exam: StudentsExam)

  def apply(): Behavior[Command] = registry()

  def registry(): Behavior[Command] = {
    Behaviors.setup(context =>
      Behaviors.receiveMessage {
        case RequestExamCommand(code, displayReceiver, distributor) =>
          context.log.info("received starting exam request")
          val student = context.spawnAnonymous(Student(displayReceiver))
          distributor ! RequestExam(code, student)
          Behaviors.same
      })
  }
}
