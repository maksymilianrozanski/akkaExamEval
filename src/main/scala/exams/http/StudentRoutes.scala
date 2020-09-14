package exams.http

import akka.NotUsed
import akka.actor.typed.scaladsl.ActorContext
import akka.actor.typed.scaladsl.AskPattern.{Askable, schedulerFromActorSystem}
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Directives.{pathPrefix, _}
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import exams.http.StudentActions.{ActionPerformed, ExamToDisplay, TestCommand}
import exams.{ExamDistributor, RequestExam, Student}

import scala.concurrent.ExecutionContext.Implicits.global

class StudentRoutes(userActions: ActorRef[StudentActions.Command])
                   (
                     val context: ActorContext[_],
                     implicit val system: ActorSystem[_],
                     implicit val examDistributor: ActorRef[ExamDistributor]) {

  private implicit val timeout: Timeout = Timeout.create(system.settings.config.getDuration("my-app.routes.ask-timeout"))

  val studentRoutes: Route = {
    pathPrefix("student") {
      (pathEndOrSingleSlash & get) {
        complete(userActions.ask(TestCommand).map {
          x =>
            println("/student route")
            HttpEntity(ContentTypes.`text/plain(UTF-8)`, "no content yet, student route")
        })
      } ~ (path("start") & get) {
        complete(userActions.ask(StudentActions.RequestExamCommand("hello", _, examDistributor)).map {
          case ExamToDisplay(exam) =>
            println("start exam route")
            HttpEntity(ContentTypes.`text/plain(UTF-8)`, s"no content yet, start exam route, description: $exam")
        })
      }
    }
  }
}
