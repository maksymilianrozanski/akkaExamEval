package exams.student

import akka.actor.testkit.typed.scaladsl.{BehaviorTestKit, TestInbox}
import akka.actor.typed.scaladsl.Behaviors
import exams.distributor.ExamDistributor.{ExamDistributor, RequestExam}
import exams.data.{ExamGenerator, StudentsExam, StudentsRequest}
import exams.http.StudentActions.{DisplayedToStudent, ExamGenerated, ExamGeneratedWithToken, GeneratingFailed}
import org.scalatest.wordspec.AnyWordSpecLike

class StudentSpec extends AnyWordSpecLike {

  "Student" when {

    val token = "stub-token"
    def tokenGen(studentsExam: StudentsExam): String = token

    "receive GiveExamToStudent" should {
      val displayReceiver = TestInbox[DisplayedToStudent]()
      val testKit = BehaviorTestKit(Student(displayReceiver.ref, tokenGen))
      val exam = ExamGenerator.sampleExam("1")
      val command = GiveExamToStudent(exam)
      testKit.run(command)

      "send ExamToDisplay with generated token to displayReceiver" in
        displayReceiver.expectMessage(ExamGeneratedWithToken(exam, token))

      "have 'stopped' behavior" in
        assertResult(Behaviors.stopped)(testKit.returnedBehavior)
    }

    "receive GiveResultToStudent" should {
      val displayReceiver = TestInbox[DisplayedToStudent]()
      val testKit = BehaviorTestKit(Student(displayReceiver.ref, tokenGen))
      testKit.run(GiveResultToStudent(0.8))
      "have 'stopped' behavior" in
        assertResult(Behaviors.stopped)(testKit.returnedBehavior)
    }

    "receive RequestExamCommand" should {
      val distributor = TestInbox[ExamDistributor]()
      val displayReceiver = TestInbox[DisplayedToStudent]()
      val testKit = BehaviorTestKit(Student(displayReceiver.ref, tokenGen))
      val studentsRequest = StudentsRequest("student123", 3, "set2")
      val command = RequestExamCommand(studentsRequest, distributor.ref)
      testKit.run(command)
      "send RequestExam message to ExamDistributor" in
        distributor.expectMessage(RequestExam(command.code, testKit.ref))
      "have 'stopped' behavior" in
        assertResult(Behaviors.stopped)(testKit.returnedBehavior)
    }

    "receive GeneratingExamFailed" should {
      val displayReceiver = TestInbox[DisplayedToStudent]()
      val testKit = BehaviorTestKit(Student(displayReceiver.ref, tokenGen))
      testKit.run(GeneratingExamFailed)
      "have 'stopped' behavior" in
        assertResult(Behaviors.stopped)(testKit.returnedBehavior)

      "send message to displayReceiver" in
        displayReceiver.expectMessage(GeneratingFailed("unknown"))
    }
  }
}
