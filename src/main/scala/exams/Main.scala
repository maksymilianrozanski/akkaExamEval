package exams

import akka.NotUsed
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.adapter.TypedActorSystemOps
import akka.actor.typed.{ActorRef, ActorSystem, Behavior, Terminated}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import exams.ExamDistributor.ExamDistributor
import exams.http.{RoutesActorsPack, StudentActions, StudentRoutes2}

import scala.util.{Failure, Success}

object Main {

  private def startHttpServer(routes: Route, system: ActorSystem[_]): Unit = {
    // Akka HTTP still needs a classic ActorSystem to start
    implicit val classicSystem: akka.actor.ActorSystem = system.toClassic
    import system.executionContext

    val futureBinding = Http().newServerAt("localhost", 8080).bind(routes)
    futureBinding.onComplete {
      case Success(binding) =>
        val address = binding.localAddress
        system.log.info("Server online at http://{}:{}/", address.getHostString, address.getPort)
      case Failure(ex) =>
        system.log.error("Failed to bind HTTP endpoint, terminating system", ex)
        system.terminate()
    }
  }


  def apply(): Behavior[NotUsed] =
    Behaviors.setup { context =>
      val examEvaluator = context.spawnAnonymous(ExamEvaluator())
      implicit val generator: ActorRef[ExamDistributor] = context.spawn(ExamDistributor(examEvaluator), "distributor")
      //      val student1 = context.spawn(Student(), "student1")
      //      val student2 = context.spawn(Student(), "student2")
      //      val student3 = context.spawn(Student(), "student3")
      //      val student4 = context.spawn(Student(), "student4")
      //
      //      generator ! RequestExam(student1)
      //      generator ! RequestExam(student2)
      //      generator ! RequestExam(student3)
      //      generator ! RequestExam(student4)

      implicit val studentActions: ActorRef[StudentActions.Command] = context.spawn(StudentActions(), "studentActions")
      context.watch(studentActions)
      implicit val timeout: Timeout = Timeout.create(context.system.settings.config.getDuration("my-app.routes.ask-timeout"))
      implicit val actorPack: RoutesActorsPack = RoutesActorsPack(studentActions, context, context.system, generator, timeout)
      val routes = StudentRoutes2.createStudentRoutes(actorPack)

      startHttpServer(routes, context.system)

      Behaviors.receiveSignal {
        case (_, Terminated(_)) => Behaviors.stopped
      }
    }

  def main(args: Array[String]): Unit = {
    ActorSystem(Main(), "examEvaluator")
  }

}
