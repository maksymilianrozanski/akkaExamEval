package exams.http

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Directives.{complete, path, pathEndOrSingleSlash, pathPrefix, post, _}
import akka.http.scaladsl.server.Route
import exams.data.{CompletedExam, StudentsRequest}
import exams.http.StudentActions.{DisplayedToStudent, ExamGenerated, GeneratingFailed}

import scala.concurrent.{ExecutionContext, Future}

object StudentRoutes extends StudentsExamJsonProtocol with SprayJsonSupport {

  def studentRoutes(implicit studentsRequest: StudentsRequest => Future[DisplayedToStudent],
                    completedExam: CompletedExam => Unit, ec: ExecutionContext): Route =
    pathPrefix("student") {
      (pathEndOrSingleSlash & get) {
        complete(
          HttpEntity(ContentTypes.`text/plain(UTF-8)`, "nothing here yet"))
      } ~ post {
        path("start2") {
          examRequestedRoute
        }
      } ~ (path("evaluate") & post & extractRequest) { _ =>
        examEvalRequested
      }
    }

  private def examRequestedRoute(implicit future: StudentsRequest => Future[DisplayedToStudent], ec: ExecutionContext): Route =
    entity(as[StudentsRequest])(request => complete(future(request).map(displayedToStudentToResponse)))

  private def displayedToStudentToResponse(displayed: DisplayedToStudent): HttpResponse =
    displayed match {
      case exam: ExamGenerated =>
        HttpResponse(status = StatusCodes.OK, entity = HttpEntity(contentType = ContentTypes.`application/json`,
          DisplayedToStudentFormat.write(exam).prettyPrint))
      case reason: GeneratingFailed =>
        HttpResponse(status = StatusCodes.NotFound, entity = HttpEntity(contentType = ContentTypes.`application/json`,
          DisplayedToStudentFormat.write(reason).prettyPrint))
    }

  private def examEvalRequested(implicit future: CompletedExam => Unit): Route = {
    entity(as[CompletedExam]) {
      exam =>
        println(s"exam eval endpoint, request: $exam")
        future(exam)
        complete(HttpEntity(ContentTypes.`text/plain(UTF-8)`, "requested exam evaluation"))
    }
  }
}
