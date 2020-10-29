package exams.http

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model.{ContentTypes, StatusCodes}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import exams.data.ExamRepository.QuestionsSet
import exams.data.StubQuestions.completedExam
import exams.distributor.ExamDistributor.ExamId
import exams.evaluator.ExamEvaluator
import exams.http.RoutesRoot.AllExamResults
import exams.http.StudentActions.{DisplayedToStudent, ExamGeneratedWithToken, ExamResult3, GeneratingFailed}
import exams.http.token.TokenGenerator.{InvalidToken, TokenValidationResult, ValidMatchedToken}
import exams.shared.data.HttpRequests._
import exams.shared.data.HttpResponses.{ExamGenerated, ExamResult}
import exams.shared.data.StudentsExam
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.Future

class RoutesRootSpec extends AnyWordSpecLike with ScalatestRouteTest with StudentsExamJsonProtocol with Matchers with SprayJsonSupport {
  object ActorInteractionsStubs {
    implicit def examRequestedStub: StudentsRequest => Future[ExamGeneratedWithToken] = (request: StudentsRequest) =>
      fail(s"examRequestedStub was not expected to be called, was called with $request")

    implicit def examCompletedStub: CompletedExam => Future[DisplayedToStudent] = (exam: CompletedExam) =>
      fail(s"examCompletedStub was not expected to be called, was called with $exam")

    implicit def addingQuestionsSetStub: QuestionsSet => Unit = (set: QuestionsSet) =>
      fail(s"addingQuestionsSetStub was not expected to be called, was called with $set")

    implicit def examTokenValidatorStub(encodedToken: String, expectedId: ExamId): Either[TokenValidationResult, ValidMatchedToken] =
      fail("examTokenValidator not expected to be called")

    implicit def examResultsStub: AllExamResults = () =>
      fail("examResults was not expected to be called")
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

      import ActorInteractionsStubs.{addingQuestionsSetStub, examCompletedStub, examResultsStub, examTokenValidatorStub}
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

      import ActorInteractionsStubs.{addingQuestionsSetStub, examCompletedStub, examResultsStub, examTokenValidatorStub}
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
    val path = "/student/evaluate"

    "request contains Authorization header" when {

      val validToken = "some-valid-token"
      val request = Post(path, completedExam).addHeader(RawHeader("Authorization", validToken))

      implicit def tokenValidator(encodedToken: String, expectedId: ExamId): Either[TokenValidationResult, ValidMatchedToken]
      = Right(ValidMatchedToken(expectedId))

      "examId in token matches request's examId" should {
        "call examCompleted action" in {
          var calledTimes = 0
          implicit def examCompletedAction: CompletedExam => Future[DisplayedToStudent] = (exam: CompletedExam) => {
            require(completedExam == exam, s"expected $completedExam, received: $exam")
            calledTimes = calledTimes + 1
            Future(ExamResult3(ExamResult("exam123", "student123", 0.8)))
          }

          import ActorInteractionsStubs.{addingQuestionsSetStub, examRequestedStub, examResultsStub}
          val route = RoutesRoot.allRoutes

          request ~> route ~> check(assertResult(1)(calledTimes))
        }

        "returned response" should {
          import ActorInteractionsStubs.{addingQuestionsSetStub, examRequestedStub, examResultsStub}
          implicit def examCompletedAction: CompletedExam => Future[DisplayedToStudent] = (exam: CompletedExam) => {
            require(completedExam == exam, s"expected $completedExam, received: $exam")
            Future(ExamResult3(ExamResult("exam123", "student123", 0.8)))
          }
          val route = RoutesRoot.allRoutes

          "have `application/json` content type" in {
            request ~> route ~> check(contentType shouldBe ContentTypes.`application/json`)
          }

          "have OK status code" in
            request ~> route ~> check(status shouldBe StatusCodes.OK)

          "have expected content" in
            request ~> route ~> check(responseAs[ExamResult] shouldBe ExamResult("exam123", "student123", 0.8))
        }
      }

      "examId in token does not match request's examId" should {
        implicit def tokenValidator(encodedToken: String, expectedId: ExamId): Either[TokenValidationResult, ValidMatchedToken]
        = Left(InvalidToken)

        val request = Post(path, completedExam).addHeader(RawHeader("Authorization", "invalid-token"))
        import ActorInteractionsStubs.{addingQuestionsSetStub, examCompletedStub, examRequestedStub, examResultsStub}
        val route = RoutesRoot.allRoutes
        "respond with unauthorized status code" in
          request ~> route ~> check(status shouldBe StatusCodes.Unauthorized)
      }
    }

    "request does not contain Authorization header" should {
      val request = Post(path, completedExam)
      import ActorInteractionsStubs._
      val route = RoutesRoot.allRoutes
      "respond with unauthorized status code" in
        request ~> route ~> check(status shouldBe StatusCodes.Unauthorized)
    }
  }
}
