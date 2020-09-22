package exams

import akka.actor.testkit.typed.scaladsl.{ScalaTestWithActorTestKit, TestInbox}
import akka.actor.typed.scaladsl.Behaviors
import akka.persistence.testkit.scaladsl.EventSourcedBehaviorTestKit
import akka.persistence.testkit.scaladsl.EventSourcedBehaviorTestKit.SerializationSettings.disabled
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.EventSourcedBehavior
import exams.ExamDistributor._
import exams.data.TeachersExam
import org.scalatest.BeforeAndAfterEach
import org.scalatest.wordspec.AnyWordSpecLike

class ExamDistributorSpec
  extends ScalaTestWithActorTestKit(EventSourcedBehaviorTestKit.config) with AnyWordSpecLike
    with BeforeAndAfterEach {

  private def generator(id: String) = TeachersExam(id, List())

  def requestExamTestKit(initialState: ExamDistributorState): EventSourcedBehaviorTestKit[RequestExam, ExamAdded, ExamDistributorState] =
    EventSourcedBehaviorTestKit(system, Behaviors.setup[RequestExam] { context =>
      EventSourcedBehavior(
        persistenceId = PersistenceId.ofUniqueId("uniqueId"),
        emptyState = initialState,
        commandHandler = onRequestExam(context)(generator) _,
        eventHandler = examAddedHandler
      )
    }, serializationSettings = disabled)


  "ExamDistributor" must {
    //setup
    val student = TestInbox[Student]()
    //given
    val initialState = ExamDistributor.emptyState
    val studentId = "123"

    "given command and empty state" when {
    val testKit = requestExamTestKit(initialState)
    val command = ExamDistributor.RequestExam(studentId, student.ref)

      "ExamDistributor" must {
        //when
        val result = testKit.runCommand(command)
        "persist ExamAdded" in {
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
    //given
    val nonEmptyState = ExamDistributorState(Map("123" -> PersistedExam("1234", generator("123"), None)))
    val studentId = "124"

    "given command and non-empty state" when {
      val testKit = requestExamTestKit(nonEmptyState)
      val command = ExamDistributor.RequestExam(studentId, student.ref)

      "ExamDistributor" must {
        //when
        val result = testKit.runCommand(command)
        "persist ExamAdded" in {
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
