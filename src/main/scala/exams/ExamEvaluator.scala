package exams

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import exams.data.{CompletedExam, EmptyExam}

sealed trait ExamEvaluatorWaiting
final case class EvaluateAnswers(student: ActorRef[Student], completedExam: CompletedExam) extends ExamEvaluatorWaiting

object ExamEvaluatorWaiting {
  def apply(emptyExam: EmptyExam): Behavior[ExamEvaluatorWaiting] = evaluator(emptyExam)

  def evaluator(emptyExam: EmptyExam): Behavior[ExamEvaluatorWaiting] = Behaviors.receive {
    case (context, EvaluateAnswers(student, completedExam)) =>
      context.log.info("Sending exam result to student")
      student ! GiveResultToStudent(0.8)
      Behaviors.stopped
  }
}
