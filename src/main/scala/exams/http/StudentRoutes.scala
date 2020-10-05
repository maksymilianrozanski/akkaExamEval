package exams.http

import akka.http.scaladsl.client.RequestBuilding.WithTransformation
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpRequest, HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Directives.{complete, path, pathEndOrSingleSlash, pathPrefix, post, _}
import akka.http.scaladsl.server.Route
import exams.data.{CompletedExam, StudentsRequest}
import exams.distributor.ExamDistributor.ExamId
import exams.http.StudentActions.{DisplayedToStudent, ExamGeneratedWithToken, GeneratingFailed}
import exams.http.token.TokenGenerator.{TokenValidationResult, ValidToken}

import scala.concurrent.{ExecutionContext, Future}

object StudentRoutes extends StudentsExamJsonProtocol with SprayJsonSupport {

  def studentRoutes(implicit studentsRequest: StudentsRequest => Future[DisplayedToStudent],
                    completedExam: CompletedExam => Unit, ec: ExecutionContext,
                    examTokenValidator: (String, ExamId) => Either[TokenValidationResult, ValidToken]): Route =
    pathPrefix("student") {
      (pathEndOrSingleSlash & get) {
        complete(
          HttpEntity(ContentTypes.`text/plain(UTF-8)`, "nothing here yet"))
      } ~ post {
        path("start2") {
          examRequestedRoute
        }
      } ~ (path("evaluate") & post & extractRequest) {
        e: HttpRequest =>
          entity(as[CompletedExam]) { exam: CompletedExam =>
            optionalHeaderValueByName("Authorization") {
              case Some(token) => examTokenValidator(token, exam.examId) match {
                case Right(ValidToken(examId)) => examEvalRequested
                case Left(value) => complete(HttpResponse(StatusCodes.Unauthorized))
              }
              case None => complete(HttpResponse(StatusCodes.Unauthorized))
            }
          }
      }
    }

  private def examRequestedRoute(implicit future: StudentsRequest => Future[DisplayedToStudent], ec: ExecutionContext): Route =
    entity(as[StudentsRequest])(request => complete(future(request).map(displayedToStudentToResponse)))

  private def displayedToStudentToResponse(displayed: DisplayedToStudent): HttpResponse =
    displayed match {
      case exam: ExamGeneratedWithToken =>
        HttpResponse(status = StatusCodes.OK, entity = HttpEntity(contentType = ContentTypes.`application/json`,
          DisplayedToStudentFormat.write(exam).prettyPrint), headers = Seq(RawHeader("Access-Token", exam.token)))
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
