package exams.http

import akka.actor.testkit.typed.scaladsl.TestInbox
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import exams.ExamDistributor.ExamDistributor
import exams.data.StudentsExam
import exams.http.HttpUtils.ScalaTestWithActorTestKitWithRouteTest
import exams.http.StudentActions.ExamToDisplay
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.duration.DurationInt

class StudentRoutes2Spec extends ScalaTestWithActorTestKitWithRouteTest with StudentsExamJsonProtocol with SprayJsonSupport with AnyWordSpecLike with Matchers {

  import exams.data.StubQuestions._

  "/student/start route" when {
    val examToDisplay = StudentsExam("exam123", List(question1.blank, question2.blank))

    val expectedMsgContent = "hello"
    val mockedBehavior = Behaviors.receiveMessage[StudentActions.Command] {
      case StudentActions.RequestExamCommand(code, replyTo) =>
        if (code == expectedMsgContent)
          replyTo ! ExamToDisplay(examToDisplay)
        else fail(s"unexpected message content: $code, expected: $expectedMsgContent")
        Behaviors.same
      case message => fail(s"unexpected message matched in mockedBehavior $message")
    }
    val studentProbe = testKit.createTestProbe[StudentActions.Command]()
    val mockedPublisher = testKit.spawn(Behaviors.monitor(studentProbe.ref, mockedBehavior))

    val actorsPack = RoutesActorsPack(mockedPublisher.ref, systemTyped, TestInbox[ExamDistributor]().ref, Timeout(2 seconds))
    val routes: Route = StudentRoutes2.createStudentRoutes(actorsPack)

    "receive get request" should {
      "send ask to StudentActions and complete with ExamToDisplay" in {
        Get("/student/start") ~> routes ~> check {
          status shouldBe StatusCodes.OK
          responseAs[ExamToDisplay] shouldBe ExamToDisplay(examToDisplay)
        }
      }
    }
  }
}

