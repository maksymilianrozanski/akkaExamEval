package exams

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import exams.ExamDistributor.Answers
import exams.data.TeachersExam

sealed trait ExamEvaluator
final case class EvaluateAnswers(examId: String, studentId: String, teachersExam: TeachersExam, answers: Answers) extends ExamEvaluator

object ExamEvaluator {
  def apply(): Behavior[ExamEvaluator] = evaluator()

  def evaluator(): Behavior[ExamEvaluator] = Behaviors.receive {
    case (context, EvaluateAnswers(examId, studentId, teachersExam, answers)) =>
      context.log.info("Received exam evaluation request")
      val result = percentOfCorrectAnswers(teachersExam, answers)
      context.log.info("exam {} of student {} result: {}", examId, studentId, result)
      Behaviors.same
  }

  private def percentOfCorrectAnswers(teachersExam: TeachersExam, answers: Answers) = {
    val validAnswers = teachersExam.questions.map(_.correctAnswers).map(_.map(_.toString))
    assert(validAnswers.nonEmpty, "exam should contain at least one question")
    assert(answers.length == validAnswers.length, "length of student's answers should be equal to list of valid answers")
    percentOfPoints(validAnswers, answers)
  }

  private def percentOfPoints[T](validAnswers: List[T], studentsAnswers: List[T]) = {
    val points = validAnswers.zip(studentsAnswers).map(
      pair => {
        println(s"correct answer: ${pair._1}, selected answer: ${pair._2}, point?:${pair._1 == pair._2}")
        if (pair._1.toString == pair._2.toString) 1 else 0
      }
    ).sum
    points.toDouble / validAnswers.length.toDouble
  }
}
