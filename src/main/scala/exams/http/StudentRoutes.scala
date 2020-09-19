package exams.http

import akka.actor.typed.scaladsl.ActorContext
import akka.actor.typed.scaladsl.AskPattern.{Askable, schedulerFromActorSystem}
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpRequest}
import akka.http.scaladsl.server.Directives.{pathPrefix, _}
import akka.http.scaladsl.server.{Route, StandardRoute}
import akka.util.Timeout
import exams.data.TeachersExam.toStudentsExam
import exams.data.{CompletedExam, ExamGenerator}
import exams.{ExamDistributor, RequestExamEvaluation}
import exams.http.StudentActions.ExamToDisplay

import scala.concurrent.ExecutionContext.Implicits.global

case class RoutesActorsPack(userActions: ActorRef[StudentActions.Command],
                            context: ActorContext[_],
                            system: ActorSystem[_],
                            examDistributor: ActorRef[ExamDistributor],
                            implicit val timeout: Timeout)

class StudentRoutes(implicit val actors: RoutesActorsPack) {

  implicit val actorSystem: ActorSystem[_] = actors.system

  val studentRoutes: Route = StudentRoutes2.studentRoutes
}

object StudentRoutes2 {


  def studentRoutes(implicit actors: RoutesActorsPack, actorSystem: ActorSystem[_]): Route = {
    pathPrefix("student") {
      (pathEndOrSingleSlash & get) {
        complete(
          HttpEntity(ContentTypes.`text/plain(UTF-8)`, "nothing here yet"))
      } ~ (path("start") & get) {
        examRequestedRoute
      } ~ (path("evaluate") & post & extractRequest) {
        examEvalRequested
      }
    }
  }

  def examRequestedRoute(implicit actors: RoutesActorsPack, actorSystem: ActorSystem[_]): StandardRoute = {
    import actors._
    complete(userActions.ask(StudentActions.RequestExamCommand("hello", _, examDistributor)).map {
      case ExamToDisplay(exam) =>
        println("start exam route")
        HttpEntity(ContentTypes.`text/plain(UTF-8)`, s"no content yet, start exam route, description: $exam")
    })
  }

  def examEvalRequested(request: HttpRequest)(implicit actors: RoutesActorsPack): Route = {
    //todo: replace generated exam with parsed JSON
    actors.examDistributor ! RequestExamEvaluation(CompletedExam(toStudentsExam(ExamGenerator.sampleExam())))
    println(s"exam eval endpoint, request: $request")
    complete(HttpEntity(ContentTypes.`text/plain(UTF-8)`, "exam eval endpoint, no content yet"))
  }
}
