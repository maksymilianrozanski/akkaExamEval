package exams.http

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.headers.BasicHttpCredentials
import akka.http.scaladsl.model.{ContentTypes, StatusCodes}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import exams.data.ExamRepository.QuestionsSet
import exams.data.StubQuestions.{question2, question3}
import exams.shared.data.HttpResponses.ExamResult
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import spray.json.DefaultJsonProtocol

import scala.concurrent.Future

class RepoRoutesSpec extends AnyWordSpecLike with ScalatestRouteTest with StudentsExamJsonProtocol with DefaultJsonProtocol with Matchers with SprayJsonSupport {
  implicit def addingQuestionsSetStub: QuestionsSet => Unit = (set: QuestionsSet) =>
    fail(s"addingQuestionsSetStub was not expected to be called, was called with $set")

  "/repo/add endpoint" when {

    implicit def examResultsStub(): Future[List[ExamResult]] =
      fail("examResults was not expected to be called")

    val questionsSet = QuestionsSet("set2", "example set description", Set(question2, question3))
    val path = "/repo/add"
    val validPassword = Auth.secretPass
    "provided valid credentials" should {
      val route = RepoRoutes.repoRoutes((_: QuestionsSet) => (), examResultsStub)
      val requestValidCredentials = Post(path, questionsSet) ~> addCredentials(BasicHttpCredentials("adm", validPassword))

      "call addingQuestions action" in {
        var calledTimes = 0
        implicit def addingQuestionsActionWithCounter: QuestionsSet => Unit = (set: QuestionsSet) => {
          require(questionsSet == set, s"expected: $questionsSet, received $set")
          calledTimes = calledTimes + 1
        }
        val routeWithCounter = RepoRoutes.repoRoutes(addingQuestionsActionWithCounter, examResultsStub)

        requestValidCredentials ~> routeWithCounter ~> check(assertResult(1)(calledTimes))
      }

      "returned response" should {
        "have `text/plain(UTF-8)` content type" in
          requestValidCredentials ~> route ~> check(contentType shouldBe ContentTypes.`text/plain(UTF-8)`)

        "have expected content" in
          requestValidCredentials ~> route ~> check(status shouldBe StatusCodes.OK)
      }
    }

    "provided invalid credentials" should {

      val route = RepoRoutes.repoRoutes(addingQuestionsSetStub, examResultsStub)
      val requestInvalidCredentials = Post(path, questionsSet) ~> addCredentials(BasicHttpCredentials("adm", s"invalid+$validPassword"))

      "return unauthorized access code" in
        requestInvalidCredentials ~> route ~> check(status shouldBe StatusCodes.Unauthorized)

      "not call inner functions" in
        requestInvalidCredentials ~> route ~> check {}
    }
  }

  "inner repo route /results endpoint" should {
    val path = "/results"
    val password = "somePassword"
    val request = Get(path) ~> addCredentials(BasicHttpCredentials("adm", password))
    val results = List(ExamResult("exam123", "student123", 0.9), ExamResult("exam124", "student124", 0.92))
    implicit def allExamResults(): Future[List[ExamResult]] = Future(results)
    val route = RepoRoutes.innerRepoRoutes(addingQuestionsSetStub, allExamResults)
    "respond with all stored exam results" in
      request ~> route ~> check {
        status shouldBe StatusCodes.OK
        responseAs[List[ExamResult]] shouldBe results
        contentType shouldBe ContentTypes.`application/json`
      }
  }
}
