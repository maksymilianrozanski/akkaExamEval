package exams.student

import akka.actor.testkit.typed.scaladsl.{BehaviorTestKit, TestInbox}
import akka.actor.typed.scaladsl.Behaviors
import exams.ExamDistributor.{ExamDistributor, RequestExam2}
import exams.data.{ExamGenerator, StudentsRequest}
import exams.http.StudentActions.ExamToDisplay
import org.scalatest.wordspec.AnyWordSpecLike

class StudentSpec extends AnyWordSpecLike {

  "Student" when {
    "receive GiveExamToStudent" should {
      val displayReceiver = TestInbox[ExamToDisplay]()
      val testKit = BehaviorTestKit(Student(displayReceiver.ref))
      val exam = ExamGenerator.sampleExam("1")
      val command = GiveExamToStudent(exam)
      testKit.run(command)
      "send ExamToDisplay to displayReceiver" in
        displayReceiver.expectMessage(ExamToDisplay(exam))
    }

    "receive RequestExamCommand" should {
      val distributor = TestInbox[ExamDistributor]()
      val displayReceiver = TestInbox[ExamToDisplay]()
      val testKit = BehaviorTestKit(Student(displayReceiver.ref))
      val studentsRequest = StudentsRequest("student123", 3, "set2")
      val command = RequestExamCommand(studentsRequest, distributor.ref)
      testKit.run(command)
      "send RequestExam message to ExamDistributor" in
        distributor.expectMessage(RequestExam2(command.code, testKit.ref))
      "have 'same' behavior" in
        assertResult(Behaviors.same)(testKit.returnedBehavior)
    }

    "receive GeneratingExamFailed" should {
      val displayReceiver = TestInbox[ExamToDisplay]()
      val testKit = BehaviorTestKit(Student(displayReceiver.ref))
      testKit.run(GeneratingExamFailed)
      "have 'same' behavior" in
        assertResult(Behaviors.same)(testKit.returnedBehavior)
    }
  }
}
