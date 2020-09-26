package exams.data

import akka.actor.testkit.typed.scaladsl.{BehaviorTestKit, TestInbox}
import exams.ExamDistributor.ExamDistributor
import exams.data.ExamGenerator.{ExamOutput, ReceivedExamRequest, ReceivedSetFromRepo, State}
import exams.data.ExamRepository.{ExamRepository, QuestionsSet, TakeQuestionsSet}
import org.scalatest.wordspec.AnyWordSpecLike

class ExamGeneratorSpec extends AnyWordSpecLike {

  private val examRequest1 = ExamRequest("exam12", "student12", 2, "set1")
  private val examRequest2 = ExamRequest("exam13", "student13", 3, "set2")
  private val examRequest3 = ExamRequest("exam14", "student14", 3, "set2")

  "ExamGenerator" when {
    val repository = TestInbox[ExamRepository]()
    val distributor = TestInbox[ExamOutput]()

    "receive ExamRequest" should {
      val examRequest = ExamRequest("exam123", "student123", 2, "set2")
      val message = ReceivedExamRequest(examRequest)

      val testKit = BehaviorTestKit(ExamGenerator(repository.ref)(distributor.ref)(State(Set.empty[ExamRequest])))
      testKit.run(message)

      "send message to repository, and message received by repository" should {
        val receivedByRepository = repository.receiveMessage()

        "contain correct setId" in {
          receivedByRepository match {
            case TakeQuestionsSet(setId, _, _) =>
              assertResult(examRequest.setId)(setId)
            case _ => fail(s"$receivedByRepository should match to TakeQuestionsSet")
          }
        }

        "contain correct examId" in {
          receivedByRepository match {
            case TakeQuestionsSet(_, examId, _) => assertResult(examRequest.examId)(examId)
            case _ => fail(s"$receivedByRepository should match to TakeQuestionsSet")
          }
        }

        "contain ref to ExamGenerator" in {
          receivedByRepository match {
            case TakeQuestionsSet(_, _, replyTo) =>
              val testMessage = ("testExamId", None)
              //sending message to replyTo passed to TakeQuestionsSet should be received by testKit
              replyTo ! testMessage
              val receivedAsString = testKit.selfInbox().receiveAll().head.toString
              assert(receivedAsString.contains(s"${testMessage._1},${testMessage._2}"))
            case _ => fail(s"$receivedByRepository should match to TakeQuestionsSet")
          }
        }
      }

      "add ExamRequest to state" in {

      }
    }

    "receive questions set" should {
      import StubQuestions._

      val initialState = State(Set(examRequest1, examRequest2, examRequest3))
      val questionsSetFromRepo = (examRequest1.examId, Some(QuestionsSet(examRequest1.setId, "set description", Set(question1, question2))))
      val message = ReceivedSetFromRepo(questionsSetFromRepo)

      val repository = TestInbox[ExamRepository]()
      val distributor = TestInbox[ExamOutput]()
      val testKit = BehaviorTestKit(ExamGenerator(repository.ref)(distributor.ref)(initialState))

      testKit.run(message)

      "send generated TeachersExam to ExamDistributor" in {
        val distributorMessage = distributor.receiveMessage()
        assertResult(examRequest1)(distributorMessage._1)

        distributorMessage._2 match {
          case Some(TeachersExam(examId, questions)) =>
            assertResult(examRequest1.examId)(examId)
            assertResult(questionsSetFromRepo._2.get.questions,
              "Generated TeachersExam should contain the same questions as questions in message from repository")(questions.toSet)
          case None =>
            fail("generatedExam should match to Some")
        }
      }

      "remove ExamRequest from state" in {

      }
    }
  }

  "createExam" when {
    import StubQuestions._
    val set = QuestionsSet("set3", "set's description", Set(question1, question2, question3, question4))
    val examFromQuestionsSet = ExamGenerator.createExam(set) _

    "maxQuestions is lower than size of set" must {
      val request = ExamRequest("exam123", "student123", 3, "set3")
      val result = examFromQuestionsSet(request)

      "create exam containing request.maxQuestions questions" in {
        assertResult(3)(result.questions.length)
        assert(result.questions.forall(set.questions.contains))
      }
      "create exam with correct examId" in assertResult(request.examId)(result.examId)
    }

    "maxQuestions is equal size of set" should {
      val request = ExamRequest("exam124", "student124", 4, "set3")
      val result = examFromQuestionsSet(request)
      "create exam containing all set's questions" in {
        assertResult(4)(result.questions.length)
        assert(result.questions.forall(set.questions.contains))
      }
      "create exam with correct examId" in assertResult(request.examId)(result.examId)
    }

    "maxQuestions is higher than size of set" should {
      val request = ExamRequest("exam124", "student124", 5, "set3")
      val result = examFromQuestionsSet(request)
      "create exam all questions from the set" in {
        assertResult(4)(result.questions.length)
        assert(result.questions.forall(set.questions.contains))
      }
      "create exam with correct examId" in assertResult(request.examId)(result.examId)
    }
  }
}
