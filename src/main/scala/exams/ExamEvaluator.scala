package exams

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import exams.data.{CompletedExam, EmptyExam, TeachersExam}

sealed trait ExamEvaluator
final case class EvaluateAnswers(student: ActorRef[Student], selectedAnswers: CompletedExam) extends ExamEvaluator

object ExamEvaluator {
  def apply(emptyExam: TeachersExam): Behavior[ExamEvaluator] = evaluator(emptyExam)

  def evaluator(emptyExam: TeachersExam): Behavior[ExamEvaluator] = Behaviors.receive {
    case (context, EvaluateAnswers(student, completedExam)) =>
      context.log.info("Sending exam result to student")
      student ! GiveResultToStudent(0.8)
      Behaviors.stopped
  }
}
