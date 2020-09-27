package exams

import akka.actor.testkit.typed.scaladsl.{ScalaTestWithActorTestKit, TestInbox}
import akka.actor.typed.scaladsl.Behaviors
import akka.persistence.testkit.scaladsl.EventSourcedBehaviorTestKit
import akka.persistence.testkit.scaladsl.EventSourcedBehaviorTestKit.SerializationSettings.disabled
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.EventSourcedBehavior
import exams.ExamDistributor._
import exams.data.{Answer, BlankQuestion, Question, TeachersExam}
import exams.evaluator.ExamEvaluator.{EvaluateAnswers, ExamEvaluator}
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
    val nonEmptyState = ExamDistributorState(Map("123" -> PersistedExam("1234", generator("123"))), Map(), Map())
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

  private val persistedExam1 = PersistedExam("student123", TeachersExam("ex123", List()))
  private val persistedExam2 = PersistedExam("student123456", TeachersExam("ex567",
    List(Question(BlankQuestion(text = "some text", answers = List(Answer("yes"), Answer("no"))), correctAnswers = List(Answer("yes"))))))
  private val persistedExam3 = PersistedExam("student123456", TeachersExam("ex568",
    List(
      Question(BlankQuestion(text = "some text", answers = List(Answer("yes"), Answer("no"))), correctAnswers = List(Answer("yes"))),
      Question(BlankQuestion(text = "some text2", answers = List(Answer("yes"), Answer("no"), Answer("None"))), correctAnswers = List(Answer("no")))))
  )

  private val emptyExams = ExamDistributor.emptyState
  private val oneExam = ExamDistributorState(Map("ex123" -> persistedExam1), Map(), Map())
  private val twoExams = ExamDistributorState(Map("ex123" -> persistedExam1, "ex567" -> persistedExam2), Map(), Map())
  private val threeExams = ExamDistributorState(Map("ex123" -> persistedExam1, "ex567" -> persistedExam2, "ex568" -> persistedExam3), Map(), Map())

  "ExamDistributor" must {
    "examAddedHandler" must {
      "add exam to empty ExamDistributorState" in {
        val examAdded = ExamAdded("student123", TeachersExam("ex123", List()))

        val result = ExamDistributor.examAddedHandler(emptyExams, examAdded)
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

        val expected = ExamDistributorState(exams = Map(
          "ex123" -> persistedExam1,
          "ex567" -> PersistedExam("student123456", TeachersExam("ex567",
            List(Question(BlankQuestion(text = "some text", answers = List(Answer("yes"), Answer("no"))), correctAnswers = List(Answer("yes"))))))), answers = Map(examCompleted.examId -> PersistedAnswers(examCompleted.answers)), Map())

        val result = ExamDistributor.examCompletedHandler(twoExams, examCompleted)
        assert(result == expected)
      }
    }

    "onExamRequestedHandler" must {
      val student1 = TestInbox[Student]()
      val existingRequest = "123" -> student1.ref
      val initialState = ExamDistributor.emptyState.copy(requests = Map(existingRequest))

      val id2: ExamId = "1234"
      val student2 = TestInbox[Student]()
      val event = ExamRequested(id2, student2.ref)
      "add request to the state" in {
        val expected = ExamDistributor.emptyState.copy(requests = Map(existingRequest, id2 -> student2.ref))
        val result = onExamRequestedHandler(initialState, event)
        assertResult(expected)(result)
      }
    }

    "onExamRequestRemovedHandler" must {
      val student1 = TestInbox[Student]()
      val student2 = TestInbox[Student]()
      val persisted1 = "1" -> student1.ref
      val persisted2 = "2" -> student2.ref
      val initialState = ExamDistributor.emptyState.copy(requests = Map(persisted1, persisted2))

      val event = ExamRequestRemoved("2")

      "remove request from the state" in {
        val expected = initialState.copy(requests = Map(persisted1))
        val result = onExamRequestRemovedHandler(initialState, event)
        assertResult(expected)(result)
      }
    }
  }

  def requestExamEvaluationTestKit(evaluator: TestInbox[ExamEvaluator])(initialState: ExamDistributorState): EventSourcedBehaviorTestKit[RequestExamEvaluation, ExamCompleted, ExamDistributorState] =
    EventSourcedBehaviorTestKit(system, Behaviors.setup[RequestExamEvaluation] { context =>
      EventSourcedBehavior(
        persistenceId = PersistenceId.ofUniqueId("uniqueId"),
        emptyState = initialState,
        commandHandler = ExamDistributor.onRequestExamEvaluation(context, evaluator.ref) _,
        eventHandler = ExamDistributor.examCompletedHandler
      )
    }, serializationSettings = disabled)

  "ExamDistributor's onRequestExamEvaluation" when {
    "answers length is equal to persisted exam's questions length, and exam with given id is persisted" must {
      val evaluator = TestInbox[ExamEvaluator]()
      val testKit = requestExamEvaluationTestKit(evaluator)(threeExams)
      val answers = List(List(Answer("yes"), Answer("no")), List(Answer("None")))
      val command = RequestExamEvaluation(persistedExam3.exam.examId, answers)

      val result = testKit.runCommand(command).events
      "send exam to exam evaluator" in {
        evaluator.expectMessage(EvaluateAnswers(persistedExam3.studentId, persistedExam3.exam, answers))
      }

      "persist ExamCompleted" in {
        val expected = Seq(ExamCompleted(persistedExam3.exam.examId, answers))
        assert(result == expected)
      }
    }

    "answers length is not equal to persisted exam's questions length" must {
      val evaluator = TestInbox[ExamEvaluator]()
      val testKit = requestExamEvaluationTestKit(evaluator)(threeExams)
      val answers = List(List(Answer("no")))
      val command = RequestExamEvaluation(persistedExam3.exam.examId, answers)

      val result = testKit.runCommand(command).events
      "not persist events" in {
        val expected = Seq()
        assert(result == expected)
      }

      "not send message to evaluator" in {
        assert(!evaluator.hasMessages)
      }
    }

    "answers id is not contained in persisted exams" must {
      val evaluator = TestInbox[ExamEvaluator]()
      val testKit = requestExamEvaluationTestKit(evaluator)(threeExams)
      val answers = List(List(Answer("yes"), Answer("no")), List(Answer("None")))
      val command = RequestExamEvaluation("invalidId", answers)
      val result = testKit.runCommand(command).events

      "not persist events" in {
        val expected = Seq()
        assert(result == expected)
      }

      "not send message to evaluator" in {
        assert(!evaluator.hasMessages)
      }
    }
  }

  "ExamDistributor" when {

    "receive RequestExam2 message" should {

      "add request to the state" in {

      }

      "send message to ExamGenerator" in {

      }
    }
  }

  "ExamDistributor" when {
    "receive message from ExamGenerator" when {
      "state contains request with given ExamId" should {

        "send message to Student" in {

        }

        "remove request from State" in {

        }
      }

      "state does not contain request with given ExamId" should {
        "stop the actor" in {

        }
      }
    }
  }
}
