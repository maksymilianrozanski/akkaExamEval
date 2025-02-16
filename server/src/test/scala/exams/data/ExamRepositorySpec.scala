package exams.data

import akka.actor.testkit.typed.scaladsl.{ScalaTestWithActorTestKit, TestProbe}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.persistence.testkit.scaladsl.EventSourcedBehaviorTestKit
import akka.persistence.testkit.scaladsl.EventSourcedBehaviorTestKit.SerializationSettings.enabled
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.EventSourcedBehavior
import exams.EventSourcedTestConfig.EventSourcedBehaviorTestKitConfigJsonSerialization
import exams.data.ExamRepository.{AddQuestionsSet, ExamRepository, ExamRepositoryEvents, ExamRepositoryState, QuestionsSetAdded, TakeQuestionsSet, TakeQuestionsSetReply}
import exams.shared.data.HttpRequests.QuestionsSet
import org.scalatest.wordspec.AnyWordSpecLike

class ExamRepositorySpec extends ScalaTestWithActorTestKit(EventSourcedBehaviorTestKitConfigJsonSerialization) with AnyWordSpecLike {

  def examRepositoryTestKit(initialState: ExamRepositoryState): EventSourcedBehaviorTestKit[
    ExamRepository, ExamRepositoryEvents, ExamRepositoryState] = {
    EventSourcedBehaviorTestKit(system, Behaviors.setup { context: ActorContext[ExamRepository] =>
      EventSourcedBehavior(
        persistenceId = PersistenceId.ofUniqueId("uniqueId"),
        emptyState = initialState,
        commandHandler = ExamRepository.commandHandler(context) _,
        eventHandler = ExamRepository.eventHandler
      )
    }, serializationSettings = enabled)
  }

  import StubQuestions._

  val persistedSets = List(
    QuestionsSet("1", "test set1", Set(question1, question2)),
    QuestionsSet("2", "test set2", Set(question3)))
  "ExamRepository" when {

    val initialState = ExamRepositoryState(persistedSets)

    "state does not contain set with command's setId and questions set is not empty" should {
      val setToSave = QuestionsSet("3", "new set", Set(question4))
      val testKit = examRepositoryTestKit(initialState)
      val expected = QuestionsSetAdded(setToSave)
      val command = AddQuestionsSet(setToSave)
      val result = testKit.runCommand(command)

      "persist QuestionsSetAdded added event" in {
        assertResult(expected)(result.event)
      }
    }

    "state contains set with command's setId" should {
      val withExistingId = QuestionsSet("2", "new set", Set(question4))
      val testKit = examRepositoryTestKit(initialState)
      val command = AddQuestionsSet(withExistingId)
      val result = testKit.runCommand(command)
      "not persist events" in {
        assertResult(Seq())(result.events)
      }
    }

    "questions set is empty" should {
      val setWithNoQuestions = QuestionsSet("3", "new set", Set())
      val testKit = examRepositoryTestKit(initialState)
      val command = AddQuestionsSet(setWithNoQuestions)
      val result = testKit.runCommand(command)

      "not persist event" in {
        assertResult(Seq())(result.events)
      }
    }

    "state contains set with command's setId and questions set is empty" should {
      val withExistingIdAndEmpty = QuestionsSet("2", "new set", Set())
      val testKit = examRepositoryTestKit(initialState)
      val command = AddQuestionsSet(withExistingIdAndEmpty)
      val result = testKit.runCommand(command)

      "not persist event" in {
        assertResult(Seq())(result.events)
      }
    }

    "receive TakeQuestionsSet" should {
      "send questions set exists" in {
        val testProbe = TestProbe[TakeQuestionsSetReply]()
        val command = TakeQuestionsSet("2", "1234", testProbe.ref)

        val expected = Some(persistedSets(1))
        val testKit = examRepositoryTestKit(initialState)

        testKit.runCommand(command)
        testProbe.expectMessage((command.examId, expected))
      }

      "send None if set does not exist" in {
        val testProbe = TestProbe[TakeQuestionsSetReply]()
        val command = TakeQuestionsSet("10", "12345", testProbe.ref)
        val expected = None
        val testKit = examRepositoryTestKit(initialState)

        testKit.runCommand(command)
        testProbe.expectMessage((command.examId, expected))
      }
    }
  }

  "ExamRepository eventHandler should add" should {
    val addedSet = QuestionsSet("3", "new set", Set(question4))
    val event = QuestionsSetAdded(addedSet)
    "add questions to the state" when {
      "state is empty" in {
        val initialState = ExamRepository.emptyState
        val result = ExamRepository.eventHandler(initialState, event)
        val expected = ExamRepositoryState(List(addedSet))
        assertResult(expected)(result)
      }

      "state is not empty" in {
        val initialState = ExamRepositoryState(List(QuestionsSet("4", "set added before", Set(question1))))
        val result = ExamRepository.eventHandler(initialState, event)
        val expected = initialState.copy(questions = initialState.questions :+ addedSet)
        assertResult(expected)(result)
      }
    }
  }
}
