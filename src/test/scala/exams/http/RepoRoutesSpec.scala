package exams.http

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.headers.BasicHttpCredentials
import akka.http.scaladsl.model.{ContentTypes, StatusCodes}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import exams.data.ExamRepository.QuestionsSet
import exams.data.StubQuestions.{question2, question3}
import exams.data.{CompletedExam, StudentsRequest}
import exams.http.StudentActions.ExamGenerated
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.Future

class RepoRoutesSpec extends AnyWordSpecLike with ScalatestRouteTest with StudentsExamJsonProtocol with Matchers with SprayJsonSupport {

  object ActorInteractionsStubs {
    implicit def examRequestedStub: StudentsRequest => Future[ExamGenerated] = (request: StudentsRequest) =>
      fail(s"examRequestedStub was not expected to be called, was called with $request")

    implicit def examCompletedStub: CompletedExam => Unit = (exam: CompletedExam) =>
      fail(s"examCompletedStub was not expected to be called, was called with $exam")

    implicit def addingQuestionsSetStub: QuestionsSet => Unit = (set: QuestionsSet) =>
      fail(s"addingQuestionsSetStub was not expected to be called, was called with $set")
  }

  "/repo/add endpoint" when {

    val questionsSet = QuestionsSet("set2", "example set description", Set(question2, question3))

    val path = "/repo/add"
    val validPassword = Auth.secretPass
    implicit def addingQuestionsSet: QuestionsSet => Unit = (questionsSet: QuestionsSet) => ()
    "provided valid credentials" should {

      val route = RepoRoutes.repoRoutes

      "call addingQuestions action" in {
        var calledTimes = 0
        implicit def addingQuestionsAction: QuestionsSet => Unit = (set: QuestionsSet) => {
          require(questionsSet == set, s"expected: $questionsSet, received $set")
          calledTimes = calledTimes + 1
        }
        val routeWithCounter = RepoRoutes.repoRoutes(addingQuestionsAction)

        Post(path, questionsSet) ~> addCredentials(BasicHttpCredentials("adm", validPassword)) ~> routeWithCounter ~> check(assertResult(1)(calledTimes))
      }

      "returned response" should {

        "have `text/plain(UTF-8)` content type" in
          Post(path, questionsSet) ~> addCredentials(BasicHttpCredentials("adm", validPassword)) ~> route ~> check(contentType shouldBe ContentTypes.`text/plain(UTF-8)`)

        "have expected content" in
          Post(path, questionsSet) ~> addCredentials(BasicHttpCredentials("adm", validPassword)) ~> route ~> check(status shouldBe StatusCodes.OK)
      }
    }

    "provided invalid credentials" should {

      implicit def addingQuestionsSetStub: QuestionsSet => Unit = (set: QuestionsSet) =>
        fail(s"addingQuestionsSetStub was not expected to be called, was called with $set")

      val route = RepoRoutes.repoRoutes(addingQuestionsSetStub)

      "return unauthorized access code" in {
        Post(path, questionsSet) ~> addCredentials(BasicHttpCredentials("adm", s"invalid+$validPassword")) ~> route ~> check {
          status shouldBe StatusCodes.Unauthorized
        }
      }

      "not call inner functions" in {
        Post(path, questionsSet) ~> addCredentials(BasicHttpCredentials("adm", s"invalid+$validPassword")) ~> route ~> check {

        }
      }
    }
  }
}
