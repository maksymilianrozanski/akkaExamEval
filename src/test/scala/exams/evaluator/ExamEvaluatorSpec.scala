package exams.evaluator

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.persistence.testkit.scaladsl.EventSourcedBehaviorTestKit
import exams.data.{Answer, BlankQuestion, Question, TeachersExam}
import org.scalatest.wordspec.AnyWordSpecLike

class ExamEvaluatorSpec extends ScalaTestWithActorTestKit(EventSourcedBehaviorTestKit.config) with AnyWordSpecLike {

  "should return percent of correct answers in exam" in {
    val teachersExam = TeachersExam("exam123",
      List(
        Question(BlankQuestion("text", List(Answer("yes"), Answer("no"))), List(Answer("yes"))),
        Question(BlankQuestion("text2", List(Answer("1"), Answer("2"), Answer("3"), Answer("4"))), List(Answer("1"), Answer("3"))),
        Question(BlankQuestion("text4", List(Answer("1"), Answer("2"), Answer("3"), Answer("4"))), List(Answer("1"), Answer("4")))))

    val answers = List(
      //incorrect
      List(Answer("no")),
      //correct
      List(Answer("1"), Answer("3")),
      //missing one answer -> incorrect
      List(Answer("1"))
    )

    val result = ExamEvaluator.percentOfCorrectAnswers(teachersExam, answers)
    val expected = 0.3333d
    assert(math.abs(result - 0.3333) < 0.001, s"expected: $expected, actual: $result")
  }

  "should return percent of correct answers" should {
    "List[Int] values" in {
      val validAnswers = List(1, 2, 3, 4)
      val studentAnswers = List(1, 2, 3, 8)

      assert(math.abs(ExamEvaluator.percentOfPoints(validAnswers, studentAnswers) - 0.75d) < 0.001)
    }

    "List[List[String]] values" in {
      val validAnswers = List(List("1"), List("2", "3"))
      val studentAnswers = List(List("2"), List("2", "3"))
      assert(math.abs(ExamEvaluator.percentOfPoints(validAnswers, studentAnswers) - 0.5d) < 0.001)
    }
  }
}
