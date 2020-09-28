package exams.student

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import exams.ExamDistributor.{ExamDistributor, RequestExam, RequestExam2}
import exams.data.{StudentsExam, StudentsRequest}
import exams.http.StudentActions.ExamToDisplay

sealed trait Student
final case class RequestExamCommand(code: StudentsRequest, distributor: ActorRef[ExamDistributor]) extends Student

final case class GiveExamToStudent(emptyExam: StudentsExam) extends Student
final case class GiveResultToStudent(result: Double) extends Student
case object GeneratingExamFailed extends Student

object Student {
  def apply(displayReceiver: ActorRef[ExamToDisplay]): Behavior[Student] = stateless(displayReceiver)

  def stateless(displayReceiver: ActorRef[ExamToDisplay]): Behavior[Student] =
    Behaviors.setup(context =>
      Behaviors.receiveMessage {
        case GiveExamToStudent(emptyExam) =>
          context.log.info(s"Student received exam ${GiveExamToStudent(emptyExam)}")
          displayReceiver ! ExamToDisplay(emptyExam)
          Behaviors.stopped
        case GiveResultToStudent(result) =>
          context.log.info("Received result: {} ", result)
          Behaviors.stopped
        case RequestExamCommand(code, distributor) =>
          context.log.info("received starting exam request")
          distributor ! RequestExam2(code, context.self)
          Behaviors.same
        case GeneratingExamFailed =>
          context.log.info("student received GeneratingExamFailed message")
          Behaviors.same
      }
    )

  //  private def randomAnswers(emptyExam: StudentsExam) =
  //    CompletedExam(emptyExam.questions.map(
  //          _ => {
  //            val rand = math.random()
  //            if (rand > 0.5)
  //              List(0)
  //            else List(1)
  //          }
  //        ))
}
