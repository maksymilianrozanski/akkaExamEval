package exams.http

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.{ContentTypes, StatusCodes}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import exams.data.ExamRepository.QuestionsSet
import exams.data.{CompletedExam, StudentsExam, StudentsRequest}
import exams.http.StudentActions.ExamToDisplay
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.Future

class StudentRoutesSpec extends AnyWordSpecLike with ScalatestRouteTest with StudentsExamJsonProtocol with Matchers with SprayJsonSupport {

  private object ActorInteractionsStubs {
    implicit def examRequestedStub: StudentsRequest => Future[ExamToDisplay] = (request: StudentsRequest) =>
      fail(s"examRequestedStub was not expected to be called, was called with $request")

    implicit def examCompletedStub: CompletedExam => Unit = (exam: CompletedExam) =>
      fail(s"examCompletedStub was not expected to be called, was called with $exam")

    implicit def addingQuestionsSetStub: QuestionsSet => Unit = (set: QuestionsSet) =>
      fail(s"addingQuestionsSetStub was not expected to be called, was called with $set")
  }

  "student/start2 endpoint" should {
    import exams.data.StubQuestions._
    val examToDisplay = ExamToDisplay(StudentsExam("exam123", List(question2.blank, question3.blank)))

    val studentsRequest = StudentsRequest("student123", 2, "set3")

    implicit def examRequestedFuture: StudentsRequest => Future[ExamToDisplay] = (request: StudentsRequest) => {
      require(request == studentsRequest, s"expected: $studentsRequest, received: $request")
      Future(examToDisplay)
    }

    import ActorInteractionsStubs.{addingQuestionsSetStub, examCompletedStub}
    val route = StudentRoutes2.studentRoutes

    "return received exam" in
      Post("/student/start2", studentsRequest) ~> route ~> check(responseAs[ExamToDisplay] shouldBe examToDisplay)

    "have `application/json` content type" in
      Post("/student/start2", studentsRequest) ~> route ~> check(contentType shouldBe ContentTypes.`application/json`)

    "have StatusCode.OK" in
      Post("/student/start2", studentsRequest) ~> route ~> check(status shouldBe StatusCodes.OK)
  }
}
