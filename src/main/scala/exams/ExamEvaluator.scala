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
      val result = percentOfCorrectAnswers(emptyExam, completedExam)
      student ! GiveResultToStudent(result)
      Behaviors.stopped
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
