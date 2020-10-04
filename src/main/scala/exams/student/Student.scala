package exams.student

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import exams.distributor.ExamDistributor.{ExamDistributor, ExamId, RequestExam}
import exams.data.{StudentsExam, StudentsRequest}
import exams.http.StudentActions
import exams.http.StudentActions.{ExamGenerated, ExamGeneratedWithToken, GeneratingFailed}
import exams.http.token.TokenGenerator

sealed trait Student
final case class RequestExamCommand(code: StudentsRequest, distributor: ActorRef[ExamDistributor]) extends Student

final case class GiveExamToStudent(emptyExam: StudentsExam) extends Student
final case class GiveResultToStudent(result: Double) extends Student
case object GeneratingExamFailed extends Student

object Student {

  def apply(displayReceiver: ActorRef[StudentActions.DisplayedToStudent], tokenGen: StudentsExam => String = tokenFromExam): Behavior[Student] =
    stateless(displayReceiver, tokenGen)

  def stateless(displayReceiver: ActorRef[StudentActions.DisplayedToStudent], tokenGen: StudentsExam => String): Behavior[Student] =
    Behaviors.setup(context =>
      Behaviors.receiveMessage {
        case GiveExamToStudent(emptyExam) =>
          context.log.info(s"Student received exam ${GiveExamToStudent(emptyExam)}")
          displayReceiver ! ExamGeneratedWithToken(emptyExam, tokenGen(emptyExam))
          Behaviors.stopped
        case GiveResultToStudent(result) =>
          context.log.info("Received result: {} ", result)
          Behaviors.stopped
        case RequestExamCommand(code, distributor) =>
          context.log.info("received starting exam request")
          distributor ! RequestExam(code, context.self)
          Behaviors.stopped
        case GeneratingExamFailed =>
          context.log.info("student received GeneratingExamFailed message")
          displayReceiver ! GeneratingFailed("unknown")
          Behaviors.stopped
      }
    )

  private def tokenFromExam(exam: StudentsExam): String =
    TokenGenerator.createToken(exam.examId, 7)(System.currentTimeMillis, TokenGenerator.secretKey)
}
