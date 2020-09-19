package exams

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import exams.data.{CompletedExam, TeachersExam}

sealed trait ExamEvaluator
final case class EvaluateAnswers(teachersExam: TeachersExam, selectedAnswers: CompletedExam) extends ExamEvaluator


object ExamEvaluator {
  def apply(): Behavior[ExamEvaluator] = evaluator()

  def evaluator(): Behavior[ExamEvaluator] = Behaviors.receive {
    case (context, EvaluateAnswers(teachersExam, completedExam)) =>
      context.log.info("Sending exam result to student")
      val result = percentOfCorrectAnswers(teachersExam, completedExam)
      context.log.info(s"Exam result: $result, not saving or sending yet.")
      Behaviors.same
  }

  private def percentOfCorrectAnswers(teachersExam: TeachersExam, answers: CompletedExam): Double = {
    val validAnswers = teachersExam.questions.map(_.correctAnswer)
    assert(validAnswers.nonEmpty, "exam should contain at least one question")
    assert(answers.selectedAnswers.length == validAnswers.length, "length of student's answers should be equal to list of valid answers")
    val points = validAnswers.zip(answers.selectedAnswers).map(pair =>
      if (pair._1 == pair._2) 1 else 0
    ).sum
    points.toDouble / validAnswers.length.toDouble
  }
}
