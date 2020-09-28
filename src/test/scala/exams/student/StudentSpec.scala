package exams.student

import akka.actor.testkit.typed.scaladsl.{BehaviorTestKit, TestInbox}
import exams.ExamDistributor.{ExamDistributor, RequestExam}
import exams.http.StudentActions.ExamToDisplay
import org.scalatest.wordspec.AnyWordSpecLike

class StudentSpec extends AnyWordSpecLike {

  "Student" when {
    val distributor = TestInbox[ExamDistributor]()
    val displayReceiver = TestInbox[ExamToDisplay]()
    val testKit = BehaviorTestKit(Student(displayReceiver.ref))
    "receive RequestExamCommand" should {
      val command = RequestExamCommand("code123", distributor.ref)
      testKit.run(command)
      "send RequestExam message to ExamDistributor" in
        distributor.expectMessage(RequestExam(command.code, testKit.ref))
    }
  }
}
