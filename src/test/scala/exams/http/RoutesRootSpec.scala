package exams.http

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.{ContentTypes, StatusCodes}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import exams.data.ExamRepository.QuestionsSet
import exams.data.{Answer, CompletedExam, StudentsExam, StudentsRequest}
import exams.http.StudentActions.{DisplayedToStudent, ExamGenerated, GeneratingFailed}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.Future

class RoutesRootSpec extends AnyWordSpecLike with ScalatestRouteTest with StudentsExamJsonProtocol with Matchers with SprayJsonSupport {

  private object ActorInteractionsStubs {
    implicit def examRequestedStub: StudentsRequest => Future[ExamGenerated] = (request: StudentsRequest) =>
      fail(s"examRequestedStub was not expected to be called, was called with $request")

    implicit def examCompletedStub: CompletedExam => Unit = (exam: CompletedExam) =>
      fail(s"examCompletedStub was not expected to be called, was called with $exam")

    implicit def addingQuestionsSetStub: QuestionsSet => Unit = (set: QuestionsSet) =>
      fail(s"addingQuestionsSetStub was not expected to be called, was called with $set")
  }

  "student/start2 endpoint" when {
    "receive exam" should {
      import exams.data.StubQuestions._
      val examToDisplay = ExamGenerated(StudentsExam("exam123", List(question2.blank, question3.blank)))

      val studentsRequest = StudentsRequest("student123", 2, "set3")

      implicit def examRequestedFuture: StudentsRequest => Future[ExamGenerated] = (request: StudentsRequest) => {
        require(request == studentsRequest, s"expected: $studentsRequest, received: $request")
        Future(examToDisplay)
      }

      import ActorInteractionsStubs.{addingQuestionsSetStub, examCompletedStub}
      val route = RoutesRoot.allRoutes

      "return received exam" in
        Post("/student/start2", studentsRequest) ~> route ~> check(responseAs[ExamGenerated] shouldBe examToDisplay)

      "have `application/json` content type" in
        Post("/student/start2", studentsRequest) ~> route ~> check(contentType shouldBe ContentTypes.`application/json`)

      "have StatusCode.OK" in
        Post("/student/start2", studentsRequest) ~> route ~> check(status shouldBe StatusCodes.OK)
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

  "student/evaluate endpoint" should {
    val completedExam = CompletedExam("exam123", List(List(Answer("1"), Answer("2")), List(Answer("yes"))))
    val path = "/student/evaluate"

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

  "/repo/add endpoint" should {
    import exams.data.StubQuestions._
    val questionsSet = QuestionsSet("set2", "example set description", Set(question2, question3))
    val path = "/repo/add"
    "call addingQuestions action" in {
      var calledTimes = 0
      implicit def addingQuestionsAction: QuestionsSet => Unit = (set: QuestionsSet) => {
        require(questionsSet == set, s"expected: $questionsSet, received $set")
        calledTimes = calledTimes + 1
      }
      import ActorInteractionsStubs.{examRequestedStub, examCompletedStub}
      val route = RoutesRoot.allRoutes

      Post(path, questionsSet) ~> route ~> check(assertResult(1)(calledTimes))
    }

    "returned response" should {
      implicit def addingQuestionsAction: QuestionsSet => Unit = (set: QuestionsSet) =>
        require(questionsSet == set, s"expected: $questionsSet, received $set")
      import ActorInteractionsStubs.{examRequestedStub, examCompletedStub}
      val route = RoutesRoot.allRoutes

      "have `text/plain(UTF-8)` content type" in
        Post(path, questionsSet) ~> route ~> check(contentType shouldBe ContentTypes.`text/plain(UTF-8)`)

      "have expected content" in
        Post(path, questionsSet) ~> route ~> check(status shouldBe StatusCodes.OK)
    }
  }
}
