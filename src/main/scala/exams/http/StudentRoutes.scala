package exams.http

import akka.actor.typed.scaladsl.ActorContext
import akka.actor.typed.scaladsl.AskPattern.{Askable, schedulerFromActorSystem}
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Directives.{pathPrefix, _}
import akka.http.scaladsl.server.{Route, StandardRoute}
import akka.util.Timeout
import exams.ExamDistributor
import exams.http.StudentActions.{ExamToDisplay, TestCommand}

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
        testRequestRoute
      } ~ (path("start") & get) {
        examRequestedRoute
      }
    }
  }

  def testRequestRoute(implicit actors: RoutesActorsPack, actorSystem: ActorSystem[_]): StandardRoute = {
    import actors._
    complete(userActions.ask(TestCommand).map {
      x =>
        println("/student route")
        HttpEntity(ContentTypes.`text/plain(UTF-8)`, "no content yet, student route")
    })
  }

  def examRequestedRoute(implicit actors: RoutesActorsPack, actorSystem: ActorSystem[_]): StandardRoute = {
    import actors._
    complete(userActions.ask(StudentActions.RequestExamCommand("hello", _, examDistributor)).map {
      case ExamToDisplay(exam) =>
        println("start exam route")
        HttpEntity(ContentTypes.`text/plain(UTF-8)`, s"no content yet, start exam route, description: $exam")
    })
  }
}
