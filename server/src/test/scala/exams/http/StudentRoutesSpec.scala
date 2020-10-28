package exams.http

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model.{ContentTypes, StatusCodes}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import exams.data.StubQuestions.completedExam
import exams.distributor.ExamDistributor.ExamId
import exams.evaluator.ExamEvaluator
import exams.http.StudentActions.{DisplayedToStudent, ExamResult}
import exams.http.StudentRoutes.examEvalRequested
import exams.http.token.TokenGenerator.{InvalidToken, InvalidTokenContent, ParsingError, TokenExpired, TokenValidationResult, ValidMatchedToken}
import exams.shared.data.HttpRequests.CompletedExam
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.Future

class StudentRoutesSpec extends AnyWordSpecLike with ScalatestRouteTest with StudentsExamJsonProtocol with Matchers with SprayJsonSupport {

  "examEvalRequested route" when {
    val path = "/student/evaluate"
    val token = "some-token"

    "examTokenValidator returns ValidMatchedToken" should {
      var completedExamCalledTimes = 0
      implicit def completedExamAction(completedExam: CompletedExam): Future[DisplayedToStudent] = {
        completedExamCalledTimes = completedExamCalledTimes + 1
        Future(ExamResult(ExamEvaluator.ExamResult("exam1", "student2", 0.82)))
      }
      implicit def examTokenValidator(token: String, examId: ExamId): Either[TokenValidationResult, ValidMatchedToken]
      = Right(ValidMatchedToken(examId))
      val route = examEvalRequested
      val request = Post(path, completedExam).addHeader(RawHeader("Authorization", token))

      "call completedExam future" in
        request ~> route ~> check(assertResult(1)(completedExamCalledTimes))

      "return StatusCode.OK" in
        request ~> route ~> check(status shouldBe StatusCodes.OK)

      "return appropriate message" in
        request ~> route ~> check {
          contentType shouldBe ContentTypes.`application/json`
          responseAs[ExamEvaluator.ExamResult] shouldEqual ExamEvaluator.ExamResult("exam1", "student2", 0.82)
        }
    }

    "examTokenValidator returns not ValidMatchedToken:" when {
      implicit def examCompletedStub: CompletedExam => Future[DisplayedToStudent] = (exam: CompletedExam) =>
        fail(s"examCompletedStub was not expected to be called, was called with $exam")

      val request = Post(path, completedExam).addHeader(RawHeader("Authorization", token))

      "examTokenValidator returns InvalidToken" should {
        implicit def examTokenValidator(token: String, examId: ExamId): Either[TokenValidationResult, ValidMatchedToken] = Left(InvalidToken)
        val route = examEvalRequested
        "return StatusCode.Unauthorized" in
          request ~> route ~> check(status shouldBe StatusCodes.Unauthorized)

        "return appropriate message" in
          request ~> route ~> check {
            contentType shouldBe ContentTypes.`text/plain(UTF-8)`
            responseAs[String] shouldEqual "server was not able to process the token"
          }
      }

      "examTokenValidator returns ParsingError" should {
        implicit def examTokenValidator(token: String, examId: ExamId): Either[TokenValidationResult, ValidMatchedToken] = Left(ParsingError)
        val route = examEvalRequested
        "return bad request StatusCode" in
          request ~> route ~> check(status shouldBe StatusCodes.BadRequest)

        "return appropriate message" in
          request ~> route ~> check {
            contentType shouldBe ContentTypes.`text/plain(UTF-8)`
            responseAs[String] shouldEqual "Error during token deserialization"
          }
      }

      "examTokenValidator returns InvalidTokenContent" should {
        implicit def examTokenValidator(token: String, examId: ExamId): Either[TokenValidationResult, ValidMatchedToken] = Left(InvalidTokenContent)
        val route = examEvalRequested
        "return StatusCode.Unauthorized" in
          request ~> route ~> check(status shouldBe StatusCodes.Unauthorized)

        "return appropriate message" in
          request ~> route ~> check {
            contentType shouldBe ContentTypes.`text/plain(UTF-8)`
            responseAs[String] shouldEqual "Token was not matched to requested exam"
          }
      }

      "examTokenValidator returns TokenExpired" should {
        implicit def examTokenValidator(token: String, examId: ExamId): Either[TokenValidationResult, ValidMatchedToken] = Left(TokenExpired)
        val route = examEvalRequested
        "return StatusCode.OK" in
          request ~> route ~> check(status shouldBe StatusCodes.OK)

        "return appropriate message" in
          request ~> route ~> check {
            contentType shouldBe ContentTypes.`text/plain(UTF-8)`
            responseAs[String] shouldEqual "Exam expired"
          }
      }
    }
  }
}
