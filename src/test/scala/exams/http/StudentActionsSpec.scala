package exams.http


import akka.actor.testkit.typed.Effect.SpawnedAnonymous
import akka.actor.testkit.typed.scaladsl.{BehaviorTestKit, TestInbox}
import exams.distributor.ExamDistributor.{ExamDistributor, RequestExam, RequestExamEvaluation}
import exams.data.{Answer, StudentsRequest}
import exams.http.StudentActions.{DisplayedToStudent, ExamGenerated, RequestExamCommand2}
import exams.student.Student
import org.scalatest.matchers.should
import org.scalatest.wordspec.AnyWordSpecLike

class StudentActionsSpec extends AnyWordSpecLike with should.Matchers {

  "StudentActions" when {

    "receive RequestExamCommand2" should {
      val distributor = TestInbox[ExamDistributor]()
      val displayInbox = TestInbox[DisplayedToStudent]()
      val studentsRequest = StudentsRequest("student12", 3, "set3")
      val message = RequestExamCommand2(studentsRequest, displayInbox.ref)
      val testKit = BehaviorTestKit(StudentActions()(distributor.ref))
      testKit.run(message)
      "send spawn Student and send message to ExamDistributor when receive RequestExamCommand2" in {
        val spawnedAnonymous = testKit.expectEffectType[SpawnedAnonymous[Student]]
        val expectedMessage = RequestExam(studentsRequest, spawnedAnonymous.ref)
        distributor.expectMessage(expectedMessage)
      }
    }
  }
  "receive RequestExamEvaluation command" should {
    val inbox = TestInbox[ExamDistributor]()
    val testKit = BehaviorTestKit(StudentActions()(inbox.ref))
    val messageContent = RequestExamEvaluation("exam123",
      List(
        List(Answer("1")),
        List(Answer("2"), Answer("3"))
      ))
    testKit.run(StudentActions.SendExamToEvaluation(messageContent))
    "redirect message to ExamDistributor" in {
      inbox.expectMessage(messageContent)
    }
  }
}
