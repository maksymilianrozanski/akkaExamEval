package exams.http

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import exams.distributor.ExamDistributor.{ExamDistributor, RequestExam, RequestExamEvaluation}
import exams.evaluator.ExamEvaluator
import exams.shared.data.HttpRequests.{ExamGenerated, StudentsRequest}
import exams.shared.data.{HttpRequests, StudentsExam}
import exams.student.Student

object StudentActions {

  sealed trait Command
  final case class RequestExamCommand(studentsRequest: StudentsRequest, replyTo: ActorRef[DisplayedToStudent]) extends Command
  final case class SendExamToEvaluationCommand(exam: HttpRequests.CompletedExam, replyTo: Option[ActorRef[DisplayedToStudent]]) extends Command

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
        case SendExamToEvaluationCommand(exam, displayReceiver) =>
          val student = displayReceiver.map(it => context.spawnAnonymous(Student(it)))
          distributor ! RequestExamEvaluation(exam.examId, exam.answers, student)
          Behaviors.same
      })
  }
}
