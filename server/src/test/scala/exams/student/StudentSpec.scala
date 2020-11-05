package exams.student

import akka.actor.testkit.typed.scaladsl.{BehaviorTestKit, ManualTime, ScalaTestWithActorTestKit, TestInbox, TestProbe}
import akka.actor.typed.{PostStop, PreRestart}
import akka.actor.typed.scaladsl.Behaviors
import exams.data.ExamGenerator
import exams.distributor.ExamDistributor.{ExamDistributor, RequestExam}
import exams.http.StudentActions
import exams.http.StudentActions.{DisplayedToStudent, ExamGeneratedWithToken, GeneratingFailed}
import exams.shared.data.HttpRequests.StudentsRequest
import exams.shared.data.HttpResponses.ExamResult
import exams.shared.data.StudentsExam
import exams.student.Student.studentWithTimer
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.duration._


class StudentSpec
  extends
    ScalaTestWithActorTestKit(ManualTime.config)
    with
    AnyWordSpecLike {

  val manualTime: ManualTime = ManualTime()

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
      val examResult = ExamResult("exam12", "student12", 0.8)
      testKit.run(GiveResultToStudent(examResult))
      "have 'stopped' behavior" in
        assertResult(Behaviors.stopped)(testKit.returnedBehavior)

      "send exam result to displayReceiver" in
        displayReceiver.expectMessage(StudentActions.ExamResult3(examResult))
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

    "does not receive any message for x seconds" should {
      case object StudentStopped
      val displayReceiver = TestInbox[DisplayedToStudent]()
      val studentStoppedProbe = TestProbe[StudentStopped.type]()

      val behavior = Behaviors.setup[StudentStopped.type](context => {
        context.watch(spawn(studentWithTimer(displayReceiver.ref, tokenGen)))
        Behaviors.receiveSignal {
          case (_, signal) if signal == PostStop =>
            studentStoppedProbe.ref ! StudentStopped
            Behaviors.same
        }
      })
      spawn(behavior)

      manualTime.expectNoMessageFor(4 seconds)
      manualTime.timePasses(2 seconds)

      "stop itself" in studentStoppedProbe.expectMessage(StudentStopped)
    }
  }
}
