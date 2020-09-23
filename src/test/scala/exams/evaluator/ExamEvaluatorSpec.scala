package exams.evaluator

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.persistence.testkit.scaladsl.EventSourcedBehaviorTestKit
import org.scalatest.wordspec.AnyWordSpecLike

class ExamEvaluatorSpec extends ScalaTestWithActorTestKit(EventSourcedBehaviorTestKit.config) with AnyWordSpecLike {

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
