package exams.http


import akka.actor.testkit.typed.Effect.SpawnedAnonymous
import akka.actor.testkit.typed.scaladsl.{BehaviorTestKit, TestInbox}
import akka.actor.typed.ActorRef
import exams.ExamDistributor.{ExamDistributor, RequestExam, RequestExamEvaluationCompact}
import exams.Student
import exams.http.StudentActions.{ExamToDisplay, RequestExamCommand}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should

class StudentActionsSpec extends AnyFlatSpec with should.Matchers {

  "StudentActions" should "redirect message to ExamDistributor" in {
    val inbox = TestInbox[ExamDistributor]()
    val testKit = BehaviorTestKit(StudentActions()(inbox.ref))
    val messageContent = RequestExamEvaluationCompact("exam123", List(List("1"), List("2", "3")))
    testKit.run(StudentActions.SendExamToEvaluation(messageContent))
    inbox.expectMessage(messageContent)
  }

  "StudentActions" should "send spawn Student and send message to ExamDistributor" in {
    //setup
    val inbox = TestInbox[ExamDistributor]()
    val displayInbox = TestInbox[ExamToDisplay]()
    //given
    val message = RequestExamCommand("123", displayInbox.ref)
    val testKit = BehaviorTestKit(StudentActions()(inbox.ref))
    //when
    testKit.run(message)
    //then
    val spawnedAnonymous = testKit.expectEffectType[SpawnedAnonymous[Student]]

    val expectedMessage = RequestExam("123", spawnedAnonymous.ref)
    inbox.expectMessage(expectedMessage)
  }
}
