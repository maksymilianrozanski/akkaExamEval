package exams

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import exams.data.{CompletedExam, EmptyExam}

sealed trait ExamEvaluatorWaiting
final case class ReceivedAnswers(student: ActorRef[StudentWaitingForResult], completedExam: CompletedExam) extends ExamEvaluatorWaiting

object ExamEvaluatorWaiting {
  def apply(emptyExam: EmptyExam): Behavior[ExamEvaluatorWaiting] = evaluator(emptyExam)

  def evaluator(emptyExam: EmptyExam): Behavior[ExamEvaluatorWaiting] =
    Behaviors.receiveMessage {
      case ReceivedAnswers(student, completedExam) =>
        student ! StudentWaitingForResult(0.8)
        Behaviors.stopped
    }
}
