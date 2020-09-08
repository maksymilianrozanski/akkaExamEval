package exams

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import exams.data.{CompletedExam, EmptyExam, StudentsExam}

sealed trait Student
final case class GiveExamToStudent(emptyExam: StudentsExam, examEvaluator: ActorRef[EvaluateAnswers]) extends Student
final case class GiveResultToStudent(result: Double) extends Student

object Student {
  def apply(): Behavior[Student] = waitingForExam()

  def waitingForExam(): Behavior[Student] = Behaviors.setup(context =>
    Behaviors.receiveMessage {
      case GiveExamToStudent(emptyExam, examEvaluator) =>
        examEvaluator ! EvaluateAnswers(context.self, CompletedExam(List()))
        waitingForResult()
      case GiveResultToStudent(_) =>
        Behaviors.unhandled
    })

  def waitingForResult(): Behavior[Student] = Behaviors.receive[Student] {
    case (_, GiveExamToStudent(_, _)) => Behaviors.unhandled
    case (context, GiveResultToStudent(result)) =>
      context.log.info("Received result: {} ", result)
      Behaviors.stopped
  }
}
