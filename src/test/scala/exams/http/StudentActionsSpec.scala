package exams.http


import akka.actor.testkit.typed.Effect.SpawnedAnonymous
import akka.actor.testkit.typed.scaladsl.{BehaviorTestKit, TestInbox}
import akka.actor.typed.ActorRef
import exams.ExamDistributor.{ExamDistributor, RequestExam, RequestExam2, RequestExamEvaluation}
import exams.data.{Answer, StudentsRequest}
import exams.http.StudentActions.{ExamToDisplay, RequestExamCommand, RequestExamCommand2}
import exams.student.Student
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should

class StudentActionsSpec extends AnyFlatSpec with should.Matchers {

  "StudentActions" should "redirect message to ExamDistributor" in {
    val inbox = TestInbox[ExamDistributor]()
    val testKit = BehaviorTestKit(StudentActions()(inbox.ref))
    val messageContent = RequestExamEvaluation("exam123",
      List(
        List(Answer("1")),
        List(Answer("2"), Answer("3"))
      ))
    testKit.run(StudentActions.SendExamToEvaluation(messageContent))
    inbox.expectMessage(messageContent)
  }

  "StudentActions" should "send spawn Student and send message to ExamDistributor" in {
    //setup
    val distributor = TestInbox[ExamDistributor]()
    val displayInbox = TestInbox[ExamToDisplay]()
    //given
    val message = RequestExamCommand("123", displayInbox.ref)
    val testKit = BehaviorTestKit(StudentActions()(distributor.ref))
    //when
    testKit.run(message)
    //then
    val spawnedAnonymous = testKit.expectEffectType[SpawnedAnonymous[Student]]

    val expectedMessage = RequestExam("123", spawnedAnonymous.ref)
    distributor.expectMessage(expectedMessage)
  }

  "StudentActions" should "send spawn Student and send message to ExamDistributor when receive RequestExamCommand2" in {
    //setup
    val distributor = TestInbox[ExamDistributor]()
    val displayInbox = TestInbox[ExamToDisplay]()
    //given
    val studentsRequest = StudentsRequest("student12", 3, "set3")
    val message = RequestExamCommand2(studentsRequest, displayInbox.ref)
    val testKit = BehaviorTestKit(StudentActions()(distributor.ref))
    //when
    testKit.run(message)
    //then
    val spawnedAnonymous = testKit.expectEffectType[SpawnedAnonymous[Student]]

    val expectedMessage = RequestExam2(studentsRequest, spawnedAnonymous.ref)
    distributor.expectMessage(expectedMessage)
  }
}
