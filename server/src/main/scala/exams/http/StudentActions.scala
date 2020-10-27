package exams.http

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import exams.distributor.ExamDistributor.{ExamDistributor, RequestExam, RequestExamEvaluation}
import exams.evaluator.ExamEvaluator
import exams.shared.data.HttpRequests.{ExamGenerated, StudentsRequest}
import exams.shared.data.StudentsExam
import exams.student.Student

object StudentActions {

  sealed trait Command
  final case class RequestExamCommand(studentsRequest: StudentsRequest, replyTo: ActorRef[DisplayedToStudent]) extends Command
  //todo: add sending DisplayedToStudent ref with RequestExamEvaluation to ExamDistributor
  final case class SendExamToEvaluationCommand(exam: RequestExamEvaluation) extends Command

  sealed trait DisplayedToStudent
  final case class ExamGeneratedWithToken(exam: StudentsExam, token: String) extends DisplayedToStudent
  object ExamGeneratedWithToken {
    implicit def toExamGenerated(examGeneratedWithToken: ExamGeneratedWithToken): ExamGenerated = ExamGenerated(examGeneratedWithToken.exam)
  }
  case class GeneratingFailed(reason: String) extends DisplayedToStudent
  case class ExamResult(result: ExamEvaluator.ExamResult) extends DisplayedToStudent

  def apply()(implicit distributor: ActorRef[ExamDistributor]): Behavior[Command] = registry(distributor)

  def registry(distributor: ActorRef[ExamDistributor]): Behavior[Command] = {
    Behaviors.setup(context =>
      Behaviors.receiveMessage {
        case RequestExamCommand(studentsRequest, displayReceiver) =>
          context.log.info("received starting exam request")
          val student = context.spawnAnonymous(Student(displayReceiver))
          distributor ! RequestExam(studentsRequest, student)
          Behaviors.same
        case SendExamToEvaluationCommand(exam) =>
          distributor ! exam
          Behaviors.same
      })
  }
}
