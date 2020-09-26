package exams.data

import akka.actor.testkit.typed.scaladsl.{BehaviorTestKit, TestInbox}
import exams.ExamDistributor.ExamDistributor
import exams.data.ExamGenerator.{ReceivedExamRequest, State}
import exams.data.ExamRepository.{ExamRepository, TakeQuestionsSet}
import org.scalatest.wordspec.AnyWordSpecLike

class ExamGeneratorSpec extends AnyWordSpecLike {

  "ExamGenerator" when {
    val repository = TestInbox[ExamRepository]()
    val distributor = TestInbox[ExamDistributor]()

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

      "send generated TeachersExam to ExamDistributor" in {

      }

      "remove ExamRequest from state" in {

      }
    }
  }

  "createExam" when {

    "maxQuestions is higher or equal size of set" should {

      "create exam containing all set's questions" in {

      }
    }

    "maxQuestions is lower than size of set" should {

      "create exam containing maxQuestions number of questions" in {

      }
    }
  }
}
