package exams.data

import akka.actor.testkit.typed.scaladsl.{BehaviorTestKit, TestInbox}
import akka.actor.typed.scaladsl.Behaviors
import exams.data.ExamGenerator._
import exams.data.ExamRepository.{ExamRepository, TakeQuestionsSet}
import exams.shared.data.HttpRequests.QuestionsSet
import exams.shared.data.TeachersExam
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
      val message = ReceivedExamRequest(examRequest, distributor.ref)

      val testKit = BehaviorTestKit(generator(repository.ref)(State(Set.empty)))
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

    "receive questions set" when {
      import StubQuestions._

      val questionsSetFromRepo = (examRequest1.examId, Some(QuestionsSet(examRequest1.setId, "set description", Set(question1, question2))))

      "receive questions set from repository" when {
        "examId in message from repository exists in current state" should {
          val repository = TestInbox[ExamRepository]()
          val distributor = TestInbox[ExamOutput]()
          val initialState = State(Set(examRequest1, examRequest2, examRequest3).map((_, distributor.ref)))
          val testKit = BehaviorTestKit(generator(repository.ref)(initialState))
          val message = ReceivedSetFromRepo(questionsSetFromRepo)
          testKit.run(message)
          "send generated TeachersExam to ExamDistributor" in {
            val distributorMessage = distributor.receiveMessage()
            assertResult(examRequest1)(distributorMessage.request)

            distributorMessage.teachersExam match {
              case Some(TeachersExam(examId, questions)) =>
                assertResult(examRequest1.examId)(examId)
                assertResult(questionsSetFromRepo._2.get.questions,
                  "Generated TeachersExam should contain the same questions as questions in message from repository")(questions.toSet)
              case None =>
                fail("generatedExam should match to Some")
            }
          }

          "remove ExamRequest from state" in {
            testKit.run(message)
            assertResult(Behaviors.stopped,
              "receiving message with examId not existing in current state should stop the actor")(testKit.returnedBehavior)
          }
        }

        "examId in message from repository doesn't exist in current state" should {
          val repository = TestInbox[ExamRepository]()
          val distributor = TestInbox[ExamOutput]()
          val initialState = State(Set(examRequest1, examRequest2, examRequest3).map((_, distributor.ref)))
          val testKit = BehaviorTestKit(generator(repository.ref)(initialState))
          val message = ReceivedSetFromRepo(("unknown-id", questionsSetFromRepo._2))
          "stop the actor" in {
            testKit.run(message)
            assertResult(Behaviors.stopped,
              "receiving message with examId not existing in current state should stop the actor")(testKit.returnedBehavior)
          }
        }

        "receive response from repository with 'None' questions-set" should {
          val repository = TestInbox[ExamRepository]()
          val distributor = TestInbox[ExamOutput]()
          val initialState = State(Set(examRequest1, examRequest2, examRequest3).map((_, distributor.ref)))
          val testKit = BehaviorTestKit(generator(repository.ref)(initialState))
          val message = ReceivedSetFromRepo((examRequest1.examId, None))
          testKit.run(message)
          "send message to distributor" in {
            assertResult(ExamOutput(examRequest1, None))(distributor.receiveMessage())
          }

          "remove request from the state" in {
            testKit.run(message)
            assertResult(Behaviors.stopped,
              "receiving message with examId not existing in current state should stop the actor")(testKit.returnedBehavior)
          }
        }
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
