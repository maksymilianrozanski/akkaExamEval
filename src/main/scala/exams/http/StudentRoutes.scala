package exams.http

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Directives.{complete, path, pathEndOrSingleSlash, pathPrefix, post, _}
import akka.http.scaladsl.server.Route
import exams.data.{CompletedExam, StudentsRequest}
import exams.http.RoutesRoot.ExamTokenValidator
import exams.http.StudentActions.{DisplayedToStudent, ExamGeneratedWithToken, GeneratingFailed}

import scala.concurrent.{ExecutionContext, Future}

object StudentRoutes extends StudentsExamJsonProtocol with SprayJsonSupport {

  def studentRoutes(implicit studentsRequest: StudentsRequest => Future[DisplayedToStudent],
                    completedExam: CompletedExam => Unit, ec: ExecutionContext,
                    examTokenValidator: ExamTokenValidator): Route =
    pathPrefix("student") {
      (pathEndOrSingleSlash & get) {
        complete(
          HttpEntity(ContentTypes.`text/plain(UTF-8)`, "nothing here yet"))
      } ~ post {
        path("start2") {
          examRequestedRoute
        }
      } ~ (path("evaluate") & post & extractRequest) {
        _ => examEvalRequested
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

  import exams.http.token.TokenGenerator._

  val invalidTokenResponse = HttpResponse(status = StatusCodes.Unauthorized, entity =
    HttpEntity(ContentTypes.`text/plain(UTF-8)`,
      "server was not able to process the token"))

  val parsingTokenErrorResponse = HttpResponse(status = StatusCodes.BadRequest, entity =
    HttpEntity(ContentTypes.`text/plain(UTF-8)`, "Error during token deserialization"))

  val invalidTokenContentResponse: HttpResponse = HttpResponse(status = StatusCodes.Unauthorized,
    entity = HttpEntity(ContentTypes.`text/plain(UTF-8)`, "Token was not matched to requested exam"))

  val tokenExpiredResponse = HttpResponse(status = StatusCodes.OK,
    entity = HttpEntity(ContentTypes.`text/plain(UTF-8)`, "Exam expired"))

  private[http] def examEvalRequested(implicit future: CompletedExam => Unit, examTokenValidator: ExamTokenValidator): Route = {
    entity(as[CompletedExam]) { exam: CompletedExam =>
      optionalHeaderValueByName("Authorization") {
        case Some(token) => examTokenValidator(token, exam.examId) match {
          case Right(ValidMatchedToken(_)) =>
            println(s"exam eval endpoint, request: $exam")
            future(exam)
            complete(HttpEntity(ContentTypes.`text/plain(UTF-8)`, "requested exam evaluation"))
          case Left(InvalidToken) =>
            complete(invalidTokenResponse)
          case Left(ParsingError) =>
            complete(parsingTokenErrorResponse)
          case Left(InvalidTokenContent) =>
            complete(invalidTokenContentResponse)
          case Left(TokenExpired) => complete(tokenExpiredResponse)
          case _ => complete(HttpResponse(StatusCodes.InternalServerError))
        }
        case None => complete(HttpResponse(StatusCodes.Unauthorized))
      }
    }
  }
}
