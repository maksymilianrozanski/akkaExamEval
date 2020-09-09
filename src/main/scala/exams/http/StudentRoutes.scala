package exams.http

import akka.actor.typed.{ActorRef, ActorSystem}
import akka.actor.typed.scaladsl.AskPattern.{Askable, schedulerFromActorSystem}
import akka.http.scaladsl.server.Directives.{pathEnd, pathPrefix, _}
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import exams.http.StudentActions.{ActionPerformed, TestCommand}

import scala.concurrent.Future

class StudentRoutes(userActions: ActorRef[StudentActions.Command])(implicit val system: ActorSystem[_]) {

  private implicit val timeout: Timeout = Timeout.create(system.settings.config.getDuration("my-app.routes.ask-timeout"))

  def testCommand(): Future[ActionPerformed] =
    userActions.ask(TestCommand)

  val studentRoutes: Route =
    pathPrefix("student") {
      concat(
        pathEnd {
          concat(
            get {
              onSuccess(testCommand()) {
                response => complete(response.description)
              }
            }
          )
        }
      )
    }
}
