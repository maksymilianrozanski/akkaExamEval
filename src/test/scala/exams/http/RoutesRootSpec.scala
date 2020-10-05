package exams.http

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model.{ContentTypes, StatusCodes}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import exams.data.ExamRepository.QuestionsSet
import exams.data.{Answer, CompletedExam, StudentsExam, StudentsRequest}
import exams.http.StudentActions.{DisplayedToStudent, ExamGenerated, ExamGeneratedWithToken, GeneratingFailed}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.Future

class RoutesRootSpec extends AnyWordSpecLike with ScalatestRouteTest with StudentsExamJsonProtocol with Matchers with SprayJsonSupport {
  object ActorInteractionsStubs {
    implicit def examRequestedStub: StudentsRequest => Future[ExamGeneratedWithToken] = (request: StudentsRequest) =>
      fail(s"examRequestedStub was not expected to be called, was called with $request")

    implicit def examCompletedStub: CompletedExam => Unit = (exam: CompletedExam) =>
      fail(s"examCompletedStub was not expected to be called, was called with $exam")

    implicit def addingQuestionsSetStub: QuestionsSet => Unit = (set: QuestionsSet) =>
      fail(s"addingQuestionsSetStub was not expected to be called, was called with $set")
  }

  "student/start2 endpoint" when {
    "receive exam" should {
      import exams.data.StubQuestions._

      val token = "some-generated-token"
      val examWithToken = ExamGeneratedWithToken(StudentsExam("exam123", List(question2.blank, question3.blank)), token)
      val examToDisplay = ExamGenerated(examWithToken.exam)

      val studentsRequest = StudentsRequest("student123", 2, "set3")

      implicit def examRequestedFuture: StudentsRequest => Future[ExamGeneratedWithToken] = (request: StudentsRequest) => {
        require(request == studentsRequest, s"expected: $studentsRequest, received: $request")
        Future(examWithToken)
      }

      import ActorInteractionsStubs.{addingQuestionsSetStub, examCompletedStub}
      val route = RoutesRoot.allRoutes

      "return received exam" in
        Post("/student/start2", studentsRequest) ~> route ~> check(responseAs[ExamGenerated] shouldBe examToDisplay)

      "have `application/json` content type" in
        Post("/student/start2", studentsRequest) ~> route ~> check(contentType shouldBe ContentTypes.`application/json`)

      "have StatusCode.OK" in
        Post("/student/start2", studentsRequest) ~> route ~> check(status shouldBe StatusCodes.OK)

      "have Access-Token header with token" in {
        Post("/student/start2", studentsRequest) ~> route ~> check(
          header("Access-Token") shouldBe Some(RawHeader("Access-Token", token)))
      }
    }

    "receive GeneratingFailed" should {
      val studentsRequest = StudentsRequest("student123", 2, "set3")
      val result = GeneratingFailed("unknown")

      implicit def examRequestedFuture: StudentsRequest => Future[DisplayedToStudent] = (request: StudentsRequest) => {
        require(request == studentsRequest, s"expected: $studentsRequest, received: $request")
        Future(result)
      }

      import ActorInteractionsStubs.{addingQuestionsSetStub, examCompletedStub}
      val route = RoutesRoot.allRoutes

      "have expected message" in
        Post("/student/start2", studentsRequest) ~> route ~> check(responseAs[GeneratingFailed] shouldBe result)

      "return response with expected status code" in {
        Post("/student/start2", studentsRequest) ~> route ~> check(status shouldBe StatusCodes.NotFound)
      }

      "have `text/plain(UTF-8)` content type" in
        Post("/student/start2", studentsRequest) ~> route ~> check(contentType shouldBe ContentTypes.`application/json`)

    }
  }

  "student/evaluate endpoint" when {
    val completedExam = CompletedExam("exam123", List(List(Answer("1"), Answer("2")), List(Answer("yes"))))
    val path = "/student/evaluate"

    //todo: implement token related logic
    fail("todo: implement token related logic")
    "request contains Authorization header" when {

      "examId in token matches request's examId" should {
        "call examCompleted action" in {
          var calledTimes = 0
          implicit def examCompletedAction: CompletedExam => Unit = (exam: CompletedExam) => {
            require(completedExam == exam, s"expected $completedExam, received: $exam")
            calledTimes = calledTimes + 1
          }

          import ActorInteractionsStubs.{addingQuestionsSetStub, examRequestedStub}
          val route = RoutesRoot.allRoutes

          Post(path, completedExam) ~> route ~> check(assertResult(1)(calledTimes))
        }

        "returned response" should {
          import ActorInteractionsStubs.{addingQuestionsSetStub, examRequestedStub}
          implicit def examCompletedAction: CompletedExam => Unit = (exam: CompletedExam) => {
            require(completedExam == exam, s"expected $completedExam, received: $exam")
          }
          val route = RoutesRoot.allRoutes

          "have `text/plain(UTF-8)` content type" in
            Post(path, completedExam) ~> route ~> check(contentType shouldBe ContentTypes.`text/plain(UTF-8)`)

          "have expected content" in
            Post(path, completedExam) ~> route ~> check(status shouldBe StatusCodes.OK)
        }
      }

      "examId in token does not match request's examId" should {
        "respond with unauthorized status code" in {

        }
      }
    }

    "request does not contain Authorization header" should {
      "respond with unauthorized status code" in {

      }
    }
  }
}
