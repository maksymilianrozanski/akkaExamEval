package exams

import akka.actor.testkit.typed.scaladsl.{ScalaTestWithActorTestKit, TestInbox}
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import akka.persistence.testkit.scaladsl.EventSourcedBehaviorTestKit
import akka.persistence.testkit.scaladsl.EventSourcedBehaviorTestKit.SerializationSettings.disabled
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior}
import exams.ExamDistributor.{ExamAdded, ExamDistributorState, PersistedExam, examAddedHandler}
import exams.data.TeachersExam
import org.scalatest.BeforeAndAfterEach
import org.scalatest.wordspec.AnyWordSpecLike

class ExamDistributorSpec
  extends ScalaTestWithActorTestKit(EventSourcedBehaviorTestKit.config) with AnyWordSpecLike
    with BeforeAndAfterEach {

  def testKitBuilder[Command, Event, State](emptyState: State)(eventHandler: (State, Event) => State)(commandHandlerCurried: State => Command => Effect[Event, State]): EventSourcedBehaviorTestKit[Command, Event, State] = {

    def commandHandler(state: State, command: Command): Effect[Event, State] = commandHandlerCurried(state)(command)

    def behavior(): Behavior[Command] = {
      Behaviors.setup[Command] {
        context =>
          EventSourcedBehavior[Command, Event, State](
            persistenceId = PersistenceId.ofUniqueId("uniqueId"),
            emptyState = emptyState,
            commandHandler = commandHandler,
            eventHandler = eventHandler
          )
      }
    }

    def testKit() =
      EventSourcedBehaviorTestKit[
        Command, Event, State
      ](system, behavior(), serializationSettings = disabled)

    testKit()

  }

  "ExamDistributor" must {
    //setup
    val student = TestInbox[Student]()

    def generator(id: String) = TeachersExam(id, List())

    val initialState = ExamDistributorState(Map())
    val testKit = testKitBuilder(initialState)(examAddedHandler)(ExamDistributor.onRequestExam(generator))
    val studentId = "123"
    val command = ExamDistributor.RequestExam(studentId, student.ref)

    "given command and empty state" when {
      "ExamDistributor" must {
        //when
        val result = testKit.runCommand(command)
        "persist ExamAdded, empty initial state" in {
          //then
          val persistedEvents = result.events
          val expectedEvents = Seq(ExamAdded("0", studentId, generator("0")))
          assert(expectedEvents == persistedEvents)
        }

        "send message to the student" in {
          student.expectMessage(GiveExamToStudent(generator("0")))
        }
      }
    }

  }

  "ExamDistributor" must {
    //setup
    val student = TestInbox[Student]()

    def generator(id: String) = TeachersExam(id, List())
    val nonEmptyState = ExamDistributorState(Map("123" -> PersistedExam("1234", generator("123"), None)))

    val testKit = testKitBuilder(nonEmptyState)(examAddedHandler)(ExamDistributor.onRequestExam(generator))
    val studentId = "124"
    val command = ExamDistributor.RequestExam(studentId, student.ref)

    "given command and non-empty state" when {
      "ExamDistributor" must {
        //when
        val result = testKit.runCommand(command)
        "persist ExamAdded, non-empty initial state" in {
          //then
          val persistedEvents = result.events
          val expectedEvents = Seq(ExamAdded("1", studentId, generator("1")))
          assert(expectedEvents == persistedEvents)
        }

        "send message to the student" in {
          student.expectMessage(GiveExamToStudent(generator("1")))
        }
      }
    }
  }
}
