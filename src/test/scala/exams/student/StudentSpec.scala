package exams.student

import akka.actor.testkit.typed.scaladsl.{BehaviorTestKit, TestInbox}
import exams.ExamDistributor.{ExamDistributor, RequestExam, RequestExam2}
import exams.data.StudentsRequest
import exams.http.StudentActions.ExamToDisplay
import org.scalatest.wordspec.AnyWordSpecLike

class StudentSpec extends AnyWordSpecLike {

  "Student" when {
    val distributor = TestInbox[ExamDistributor]()
    val displayReceiver = TestInbox[ExamToDisplay]()
    val testKit = BehaviorTestKit(Student(displayReceiver.ref))
    "receive RequestExamCommand" should {
      val studentsRequest = StudentsRequest("student123", 3, "set2")
      val command = RequestExamCommand(studentsRequest, distributor.ref)
      testKit.run(command)
      "send RequestExam message to ExamDistributor" in
        distributor.expectMessage(RequestExam2(command.code, testKit.ref))
    }
  }
}
