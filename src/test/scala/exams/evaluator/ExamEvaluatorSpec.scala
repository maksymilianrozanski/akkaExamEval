package exams.evaluator

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.actor.typed.scaladsl.Behaviors
import akka.persistence.testkit.scaladsl.EventSourcedBehaviorTestKit
import akka.persistence.testkit.scaladsl.EventSourcedBehaviorTestKit.SerializationSettings.disabled
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.EventSourcedBehavior
import exams.data.{Answer, BlankQuestion, Question, TeachersExam}
import exams.evaluator.ExamEvaluator.{ExamEvaluatorState, ExamResult}
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

  "ExamEvaluator" must {
    def evaluateAnswersCommandHandlerTestKit(initialState: ExamEvaluatorState): EventSourcedBehaviorTestKit[
      ExamEvaluator.EvaluateAnswers, ExamEvaluator.ExamEvaluated, ExamEvaluator.ExamEvaluatorState] = {
      EventSourcedBehaviorTestKit(system, Behaviors.setup { context =>
        EventSourcedBehavior(
          persistenceId = PersistenceId.ofUniqueId("uniqueId"),
          emptyState = initialState,
          commandHandler = ExamEvaluator.onEvaluateExamCommand(context),
          eventHandler = ExamEvaluator.onExamEvaluatedEvent
        )
      }, serializationSettings = disabled)
    }

    "ExamEvaluator" when {
      val command = ExamEvaluator.EvaluateAnswers("exam123", "student234",
        TeachersExam("exam123", questions = List(Question(BlankQuestion("text", List(Answer("yes"), Answer("no"))), List(Answer("no"))))),
        List(List(Answer("yes"))))

      val expectedEvent = ExamEvaluator.ExamEvaluated(ExamResult("exam123", "student234", 0))

      "empty initial state" should {
        val testKit = evaluateAnswersCommandHandlerTestKit(ExamEvaluator.emptyState)
        val expected = expectedEvent
        val result = testKit.runCommand(command).event
        "return ExamEvaluated event" in {
          assert(result == expected)
        }
      }

      "non empty initial state" should {
        val initialState = ExamEvaluatorState(List(
          ExamResult("exam123", "student123", 0.2), ExamResult("exam125", "student125", 0.8)))
        val testKit = evaluateAnswersCommandHandlerTestKit(initialState)
        val expected = expectedEvent
        val result = testKit.runCommand(command).event
        "return ExamEvaluated event" in {
          assert(result == expected)
        }
      }
    }

    "event handler" must {
      val resultToSave = ExamResult("exam123", "student234", 0)
      val event = ExamEvaluator.ExamEvaluated(resultToSave)
      "add event to" must {
        "empty state" in {
          val initialState = ExamEvaluator.emptyState
          val expected = ExamEvaluatorState(List(resultToSave))
          val result = ExamEvaluator.onExamEvaluatedEvent(initialState, event)
          assert(result == expected)
        }

        "non empty state" in {
          val persistedResult = ExamResult("exam120", "student123", 0.9)
          val initialState = ExamEvaluatorState(List(persistedResult))
          val expected = ExamEvaluatorState(List(persistedResult, resultToSave))
          val result = ExamEvaluator.onExamEvaluatedEvent(initialState, event)
          assert(result == expected)
        }
      }
    }
  }
}
