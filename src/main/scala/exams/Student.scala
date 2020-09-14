package exams

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import exams.data.{CompletedExam, StudentsExam}
import exams.http.StudentActions
import exams.http.StudentActions.ExamToDisplay

sealed trait Student
final case class GiveExamToStudent(emptyExam: StudentsExam, examEvaluator: ActorRef[EvaluateAnswers]) extends Student
final case class GiveResultToStudent(result: Double) extends Student

object Student {
  def apply(displayReceiver: ActorRef[ExamToDisplay]): Behavior[Student] = waitingForExam(displayReceiver)

  def waitingForExam(displayReceiver: ActorRef[ExamToDisplay]): Behavior[Student] = Behaviors.setup(context =>
    Behaviors.receiveMessage {
      case GiveExamToStudent(emptyExam, examEvaluator) =>
        context.log.info(s"Student received exam ${GiveExamToStudent(emptyExam, examEvaluator)}")
        displayReceiver ! ExamToDisplay(emptyExam)
        waitingForResult()
      case GiveResultToStudent(_) =>
        Behaviors.unhandled
    })

  private def randomAnswers(emptyExam: StudentsExam) =
    CompletedExam(emptyExam.questions.map(
      _ => {
        val rand = math.random()
        if (rand > 0.5)
          List(0)
        else List(1)
      }
    ))

  def waitingForResult(): Behavior[Student] = Behaviors.receive[Student] {
    case (_, GiveExamToStudent(_, _)) => Behaviors.unhandled
    case (context, GiveResultToStudent(result)) =>
      context.log.info("Received result: {} ", result)
      Behaviors.stopped
  }
}
