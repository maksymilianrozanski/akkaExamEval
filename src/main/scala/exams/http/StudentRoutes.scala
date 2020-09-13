package exams.http

import akka.actor.typed.scaladsl.AskPattern.{Askable, schedulerFromActorSystem}
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Directives.{pathPrefix, _}
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import exams.http.StudentActions.TestCommand
import exams.{ExamDistributor, RequestExam}

import scala.concurrent.ExecutionContext.Implicits.global

class StudentRoutes(userActions: ActorRef[StudentActions.Command])(implicit val system: ActorSystem[_],
                                                                   implicit val examDistributor: ActorRef[ExamDistributor]
) {

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
        complete(userActions.ask(StudentActions.RequestExamCommand("hello", _)).map {
          case RequestExam(student) =>
            println("start exam route")
            HttpEntity(ContentTypes.`text/plain(UTF-8)`, "no content yet, start exam route")
        })
      }
    }
  }
}
