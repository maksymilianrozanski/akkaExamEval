package exams.http

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import exams.ExamDistributor.{ExamDistributor, RequestExam, RequestExamEvaluation}
import exams.data.StudentsExam
import exams.student.Student

object StudentActions {

  sealed trait Command
  final case class RequestExamCommand(code: String, replyTo: ActorRef[ExamToDisplay]) extends Command
  final case class SendExamToEvaluation(exam: RequestExamEvaluation) extends Command

  final case class ActionPerformed(description: String)
  final case class ExamToDisplay(exam: StudentsExam)

  def apply()(implicit distributor: ActorRef[ExamDistributor]): Behavior[Command] = registry(distributor)

  def registry(distributor: ActorRef[ExamDistributor]): Behavior[Command] = {
    Behaviors.setup(context =>
      Behaviors.receiveMessage {
        case RequestExamCommand(code, displayReceiver) =>
          context.log.info("received starting exam request")
          val student = context.spawnAnonymous(Student(displayReceiver))
          distributor ! RequestExam(code, student)
          Behaviors.same
        case SendExamToEvaluation(exam) =>
          distributor ! exam
          Behaviors.same
      })
  }
}
