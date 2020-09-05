package exams

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import exams.data.{CompletedExam, EmptyExam}

sealed trait ExamEvaluator
final case class RequestExam(student: ActorRef[Student]) extends ExamEvaluator
final case class EvaluateExam(student: ActorRef[Student], completedExam: CompletedExam) extends ExamEvaluator

object ExamEvaluator {

  def apply(): Behavior[ExamEvaluator] = evaluator()

  def evaluator(): Behavior[ExamEvaluator] = {
    Behaviors.setup(context =>
      Behaviors.receiveMessage {
        case RequestExam(student) =>
          student ! ReceivedExam(EmptyExam(), context.self)
          Behaviors.same
        case EvaluateExam(student, completedExam) =>
          student ! ExamResult(0.78)
          Behaviors.same
      })
  }
}

