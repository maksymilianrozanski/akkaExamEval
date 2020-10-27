package exams.distributor

import akka.actor.testkit.typed.scaladsl.{ScalaTestWithActorTestKit, TestInbox, TestProbe}
import akka.actor.typed.scaladsl.Behaviors
import akka.persistence.testkit.scaladsl.EventSourcedBehaviorTestKit
import akka.persistence.testkit.scaladsl.EventSourcedBehaviorTestKit.SerializationSettings.enabled
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.EventSourcedBehavior
import exams.EventSourcedTestConfig.EventSourcedBehaviorTestKitConfigJsonSerialization
import exams.data.ExamGenerator.{ExamGenerator, ExamOutput}
import exams.data._
import exams.distributor.ExamDistributor._
import exams.evaluator.ExamEvaluator.{EvaluateAnswers, ExamEvaluator}
import exams.http.StudentActions.DisplayedToStudent
import exams.shared.data
import exams.shared.data.HttpRequests.StudentsRequest
import exams.shared.data.{Answer, BlankQuestion, ExamRequest, Question, TeachersExam}
import exams.student.{GeneratingExamFailed, GiveExamToStudent, Student}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.wordspec.AnyWordSpecLike

class ExamDistributorSpec
  extends ScalaTestWithActorTestKit(EventSourcedBehaviorTestKitConfigJsonSerialization) with AnyWordSpecLike
    with BeforeAndAfterEach {

  private val persistedExam1 = PersistedExam("student123", data.TeachersExam("ex123", List()))
  private val persistedExam2 = PersistedExam("student123456", data.TeachersExam("ex567",
    List(Question(BlankQuestion(text = "some text", answers = List(Answer("yes"), Answer("no"))), correctAnswers = List(Answer("yes"))))))
  private val persistedExam3 = PersistedExam("student123456", data.TeachersExam("ex568",
    List(
      Question(BlankQuestion(text = "some text", answers = List(Answer("yes"), Answer("no"))), correctAnswers = List(Answer("yes"))),
      Question(BlankQuestion(text = "some text2", answers = List(Answer("yes"), Answer("no"), Answer("None"))), correctAnswers = List(Answer("no")))))
  )

  private val emptyExams = ExamDistributor.emptyState
  private val oneExam = ExamDistributorState(Map("ex123" -> persistedExam1), Map(), Map(), 0)
  private val twoExams = ExamDistributorState(Map("ex123" -> persistedExam1, "ex567" -> persistedExam2), Map(), Map(), 0)
  private val threeExams = ExamDistributorState(Map("ex123" -> persistedExam1, "ex567" -> persistedExam2, "ex568" -> persistedExam3), Map(), Map(), 0)

  "ExamDistributor" must {
    "examAddedHandler" must {
      "add exam to empty ExamDistributorState" in {
        val examAdded = ExamAdded("student123", data.TeachersExam("ex123", List()))

        val result = ExamDistributor.examAddedHandler(emptyExams, examAdded)
        assert(oneExam == result)
      }

      "add exam to non-empty ExamDistributorState" in {
        val examAdded = ExamAdded("student123456", TeachersExam("ex567",
          List(Question(BlankQuestion(text = "some text", answers = List(Answer("yes"), Answer("no"))), correctAnswers = List(Answer("yes"))))))

        val result = ExamDistributor.examAddedHandler(oneExam, examAdded)

        assert(twoExams == result)
      }

      "remove request from state, and add exam" in {
        val student1 = TestProbe[Student]()
        val student2 = TestProbe[Student]()

        val examAdded = ExamAdded("student123", data.TeachersExam("ex1200", List()))
        val initialState = threeExams.copy(requests = Map("ex1200" -> student1.ref, "ex1201" -> student2.ref))

        assertResult(
          expected =
            threeExams.copy(requests = Map("ex1201" -> student2.ref),
              exams = threeExams.exams.updated(examAdded.exam.examId,
                PersistedExam(examAdded.studentId, examAdded.exam))))(
          actual = ExamDistributor.examAddedHandler(initialState, examAdded))
      }
    }

    "examCompletedHandler" when {
      val examCompleted = ExamCompleted("ex567", List(List(Answer("yes"))))

      val studentInbox = TestInbox[Student]()
      val exam3Answers = persistedExam3.exam.examId -> PersistedAnswers(List(List(Answer("yes")), List(Answer("no"))))

      val twoExamsWithRequests = ExamDistributorState(
        exams = Map(
          "ex123" -> persistedExam1,
          "ex567" -> persistedExam2,
          "ex568" -> persistedExam3),
        answers = Map(exam3Answers),
        requests = Map("ex123" -> studentInbox.ref), lastExamId = 4)

      val expected = ExamDistributorState(
        exams = Map(
          "ex123" -> persistedExam1,
          "ex567" -> persistedExam2,
          "ex568" -> persistedExam3),
        answers = Map(
          examCompleted.examId -> PersistedAnswers(examCompleted.answers),
          exam3Answers),
        requests = Map("ex123" -> studentInbox.ref), lastExamId = 4)

      "processing ExamCompleted event" should {

        val result = ExamDistributor.examCompletedHandler(twoExamsWithRequests, examCompleted)

        "add answers to persisted exam" in
          assertResult(expected.answers)(result.answers)

        "not delete any exam" in
          assertResult(expected.exams)(result.exams)

        "not remove any requests" in
          assertResult(expected.requests)(result.requests)

        "not change lastExamId" in
          assertResult(expected.lastExamId)(result.lastExamId)

        "not change anything else" in {
          assert(result == expected)
        }
      }
    }

    "onExamRequestedHandler" must {
      val student1 = TestProbe[Student]()
      val existingRequest = "123" -> student1.ref
      val initialState = ExamDistributor.emptyState.copy(requests = Map(existingRequest), lastExamId = 11)

      val id2: ExamId = "1234"
      val student2 = TestProbe[Student]()
      val event = ExamRequested(id2, student2.ref)

      val result = onExamRequestedHandler(initialState, event)

      "add request to the state" in {
        val expectedRequests = Map(existingRequest, id2 -> student2.ref)
        assertResult(expectedRequests)(result.requests)
      }

      "increase lastExamId by 1" in {
        assertResult(initialState.lastExamId + 1)(result.lastExamId)
      }
    }

    "onExamRequestRemovedHandler" must {
      val student1 = TestProbe[Student]()
      val student2 = TestProbe[Student]()
      val persisted1 = "1" -> student1.ref
      val persisted2 = "2" -> student2.ref
      val initialState = ExamDistributor.emptyState.copy(requests = Map(persisted1, persisted2), lastExamId = 0)

      val event = ExamRequestRemoved("2")

      "remove request from the state" in {
        val expected = initialState.copy(requests = Map(persisted1), lastExamId = 0)
        val result = onExamRequestRemovedHandler(initialState, event)
        assertResult(expected)(result)
      }
    }
  }

  def requestExamEvaluationTestKit(evaluator: TestProbe[ExamEvaluator])(initialState: ExamDistributorState): EventSourcedBehaviorTestKit[RequestExamEvaluation, ExamCompleted, ExamDistributorState] =
    EventSourcedBehaviorTestKit(system, Behaviors.setup[RequestExamEvaluation] { context =>
      EventSourcedBehavior(
        persistenceId = PersistenceId.ofUniqueId("uniqueId"),
        emptyState = initialState,
        commandHandler = ExamDistributor.onRequestExamEvaluation(context, evaluator.ref) _,
        eventHandler = ExamDistributor.examCompletedHandler
      )
    }, serializationSettings = enabled)

  "ExamDistributor's onRequestExamEvaluation" when {
    "answers length is equal to persisted exam's questions length, and exam with given id is persisted" must {
      val evaluator = TestProbe[ExamEvaluator]()
      val displayedToStudent = TestProbe[DisplayedToStudent]()
      val testKit = requestExamEvaluationTestKit(evaluator)(threeExams)
      val answers = List(List(Answer("yes"), Answer("no")), List(Answer("None")))
      val command = RequestExamEvaluation(persistedExam3.exam.examId, answers, Some(displayedToStudent.ref))

      val result = testKit.runCommand(command).events
      "send exam to exam evaluator" in {
        evaluator.expectMessage(EvaluateAnswers(persistedExam3.studentId, persistedExam3.exam, answers, Some(displayedToStudent.ref)))
      }

      "persist ExamCompleted" in {
        val expected = Seq(ExamCompleted(persistedExam3.exam.examId, answers))
        assert(result == expected)
      }
    }

    "answers length is not equal to persisted exam's questions length" must {
      val evaluator = TestProbe[ExamEvaluator]()
      val displayedToStudent = TestProbe[DisplayedToStudent]()
      val testKit = requestExamEvaluationTestKit(evaluator)(threeExams)
      val answers = List(List(Answer("no")))
      val command = RequestExamEvaluation(persistedExam3.exam.examId, answers, Some(displayedToStudent.ref))

      val result = testKit.runCommand(command).events
      "not persist events" in {
        val expected = Seq()
        assert(result == expected)
      }

      "not send message to evaluator" in {
        evaluator.expectNoMessage()
      }
    }

    "answers id is not contained in persisted exams" must {
      val evaluator = TestProbe[ExamEvaluator]()
      val displayedToStudent = TestProbe[DisplayedToStudent]()
      val testKit = requestExamEvaluationTestKit(evaluator)(threeExams)
      val answers = List(List(Answer("yes"), Answer("no")), List(Answer("None")))
      val command = RequestExamEvaluation("invalidId", answers, Some(displayedToStudent.ref))
      val result = testKit.runCommand(command).events

      "not persist events" in {
        val expected = Seq()
        assert(result == expected)
      }

      "not send message to evaluator" in {
        evaluator.expectNoMessage()
      }
    }

    "exam was already sent to evaluation" must {
      val evaluator = TestProbe[ExamEvaluator]()
      val displayedToStudent = TestProbe[DisplayedToStudent]()
      val exam3Answers = List(List(Answer("yes"), Answer("no")), List(Answer("None")))
      val initialState = threeExams.copy(answers = Map(persistedExam3.exam.examId -> PersistedAnswers(exam3Answers)))
      val testKit = requestExamEvaluationTestKit(evaluator)(initialState)
      val command = RequestExamEvaluation(persistedExam3.exam.examId, exam3Answers, Some(displayedToStudent.ref))
      val result = testKit.runCommand(command).events

      "not persist any events" in {
        assertResult(Seq())(result)
      }

      "not send message to evaluator" in {
        evaluator.expectNoMessage()
      }
    }
  }

  def requestExamTestKit(generator: TestProbe[ExamGenerator], messageAdapter: TestInbox[ExamOutput])(initialState: ExamDistributorState)
  : EventSourcedBehaviorTestKit[RequestExam, ExamRequested, ExamDistributorState] =
    EventSourcedBehaviorTestKit(system, Behaviors.setup[RequestExam] { context =>
      EventSourcedBehavior(
        persistenceId = PersistenceId.ofUniqueId("uniqueId"),
        emptyState = initialState,
        commandHandler = onRequestExam(context)(generator.ref, messageAdapter.ref) _,
        eventHandler = onExamRequestedHandler
      )
    }, serializationSettings = enabled)

  "ExamDistributor" when {
    val generatorInbox = TestProbe[ExamGenerator]()
    val fakeMessageAdapter = TestInbox[ExamOutput]()
    val studentInbox = TestProbe[Student]()
    val initialState = ExamDistributor.emptyState.copy(lastExamId = 10)

    val studentsRequest = StudentsRequest("student123", 2, "set2")
    val command = RequestExam(studentsRequest, studentInbox.ref)
    val testKit = requestExamTestKit(generatorInbox, fakeMessageAdapter)(initialState)
    "receive RequestExam message" should {
      val expectedId = (initialState.lastExamId + 1).toString
      val event = testKit.runCommand(command).event
      "generate ExamId and return ExamRequested event" in {
        assertResult(ExamRequested(expectedId, studentInbox.ref))(event)
      }
      val receivedMessage = generatorInbox.receiveMessage()
      "send message to ExamGenerator" in {
        receivedMessage match {
          case ExamGenerator.ReceivedExamRequest(examRequest, replyTo) =>
            assertResult(ExamRequest(expectedId, studentsRequest.studentId, studentsRequest.maxQuestions,
              studentsRequest.setId))(examRequest)
            assertResult(fakeMessageAdapter.ref, "message should contain reference to message adapter")(replyTo)
          case notMatched => fail(s"should match to ExamGenerator.ReceivedExamRequest, actual: $notMatched")
        }
      }
    }
  }

  def receivedGeneratedExamTestKit(initialState: ExamDistributorState)
  : EventSourcedBehaviorTestKit[ReceivedGeneratedExam, ExamAdded, ExamDistributorState] = {
    EventSourcedBehaviorTestKit(system, Behaviors.setup[ReceivedGeneratedExam] { context =>
      EventSourcedBehavior(
        persistenceId = PersistenceId.ofUniqueId("uniqueId"),
        emptyState = initialState,
        commandHandler = onReceivingGeneratedExam(context) _,
        eventHandler = distributorEventHandler
      )
    }, serializationSettings = enabled)
  }

  "ExamDistributor" when {
    "receive message from ExamGenerator" when {
      "state contains request with given ExamId" should {
        val student1 = TestProbe[Student]()
        val student2 = TestProbe[Student]()
        val initialState = ExamDistributor.emptyState.copy(lastExamId = 20,
          requests = Map("19" -> student1.ref, "20" -> student2.ref))

        import exams.data.StubQuestions._
        val examRequest = data.ExamRequest("19", "student123", 2, "set1")
        val exam = data.TeachersExam("19", List(question1, question2))
        val command = ReceivedGeneratedExam(ExamOutput(examRequest, Some(exam)))

        val testKit = receivedGeneratedExamTestKit(initialState)
        val result = testKit.runCommand(command)

        "send message to Student" in {
          student1.expectMessage(GiveExamToStudent(exam))
        }
        "send message only to correct student" in {
          student2.expectNoMessage()
        }
        "return ExamAdded event" in {
          assertResult(ExamAdded(examRequest.studentId, exam))(result.event)
        }
      }

      "received message contains no exam" should {
        val student1 = TestProbe[Student]()
        val student2 = TestProbe[Student]()
        val initialState = ExamDistributor.emptyState.copy(lastExamId = 20,
          requests = Map("19" -> student1.ref, "20" -> student2.ref))

        val examRequest = data.ExamRequest("19", "student123", 2, "set1")
        val command = ReceivedGeneratedExam(ExamOutput(examRequest, None))
        val testKit = receivedGeneratedExamTestKit(initialState)
        testKit.runCommand(command)
        "send GeneratingExamFailed to student" in {
          student1.expectMessage(GeneratingExamFailed)
        }
      }
    }
  }
}
