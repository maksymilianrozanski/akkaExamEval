package exams

import akka.actor.testkit.typed.scaladsl.{ScalaTestWithActorTestKit, TestInbox}
import akka.actor.typed.scaladsl.Behaviors
import akka.persistence.testkit.scaladsl.EventSourcedBehaviorTestKit
import akka.persistence.testkit.scaladsl.EventSourcedBehaviorTestKit.SerializationSettings.disabled
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.EventSourcedBehavior
import exams.ExamDistributor._
import exams.data.{Answer, BlankQuestion, Question, TeachersExam}
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
          val expectedEvents = Seq(ExamAdded(studentId, generator("0")))
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
    val nonEmptyState = ExamDistributorState(Map("123" -> PersistedExam("1234", generator("123"))), Map())
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
          val expectedEvents = Seq(ExamAdded(studentId, generator("1")))
          assert(expectedEvents == persistedEvents)
        }

        "send message to the student" in {
          student.expectMessage(GiveExamToStudent(generator("1")))
        }
      }
    }
  }

  "ExamDistributor" must {

    val empty = ExamDistributor.emptyState

    val persisted1 = PersistedExam("student123", TeachersExam("ex123", List()))
    val persisted2 = PersistedExam("student123456", TeachersExam("ex567",
      List(Question(BlankQuestion(text = "some text", answers = List(Answer("yes"), Answer("no"))), correctAnswers = List(Answer("yes"))))))

    val oneExam = ExamDistributorState(Map("ex123" -> persisted1), Map())
    val twoExams = ExamDistributorState(Map("ex123" -> persisted1, "ex567" -> persisted2), Map())

    "examAddedHandler" must {

      "add exam to empty ExamDistributorState" in {
        val examAdded = ExamAdded("student123", TeachersExam("ex123", List()))

        val result = ExamDistributor.examAddedHandler(empty, examAdded)
        assert(oneExam == result)
      }

      "add exam to non-empty ExamDistributorState" in {
        val examAdded = ExamAdded("student123456", TeachersExam("ex567",
          List(Question(BlankQuestion(text = "some text", answers = List(Answer("yes"), Answer("no"))), correctAnswers = List(Answer("yes"))))))

        val result = ExamDistributor.examAddedHandler(oneExam, examAdded)

        assert(twoExams == result)
      }
    }

    "examCompletedHandler" must {

      "add answers to persisted exam" in {
        val examCompleted = ExamCompleted("ex567", List(List(Answer("yes"))))

        val expected = ExamDistributorState(
          exams = Map(
            "ex123" -> persisted1,
            "ex567" -> PersistedExam("student123456", TeachersExam("ex567",
              List(Question(BlankQuestion(text = "some text", answers = List(Answer("yes"), Answer("no"))), correctAnswers = List(Answer("yes"))))))),
          answers = Map(examCompleted.examId -> PersistedAnswers(examCompleted.answers)))

        val result = ExamDistributor.examCompletedHandler(twoExams, examCompleted)
        assert(result == expected)

      }
    }
  }
}
