package exams

import akka.NotUsed
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.adapter.TypedActorSystemOps
import akka.actor.typed.{ActorRef, ActorSystem, Behavior, Terminated}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import exams.data.{ExamGenerator, ExamRepository}
import exams.distributor.ExamDistributor
import exams.distributor.ExamDistributor.ExamDistributor
import exams.evaluator.ExamEvaluator
import exams.http.{RoutesActorsPack, RoutesRoot, StudentActions}

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
      val repository = context.spawnAnonymous(ExamRepository())
      val examGenerator = context.spawnAnonymous(ExamGenerator(repository))
      val distributorActors = ExamDistributor.ActorsPack(examEvaluator, examGenerator)
      implicit val distributor: ActorRef[ExamDistributor] = context.spawn(ExamDistributor(examEvaluator, distributorActors), "distributor")

      implicit val studentActions: ActorRef[StudentActions.Command] = context.spawn(StudentActions(), "studentActions")
      context.watch(studentActions)
      implicit val timeout: Timeout = Timeout.create(context.system.settings.config.getDuration("my-app.routes.ask-timeout"))
      implicit val actorPack: RoutesActorsPack = RoutesActorsPack(studentActions, context.system, distributor, repository, examEvaluator, timeout)
      val routes = RoutesRoot.createStudentRoutes(actorPack)

      startHttpServer(routes, context.system)

      Behaviors.receiveSignal {
        case (_, Terminated(_)) => Behaviors.stopped
      }
    }

  def main(args: Array[String]): Unit = {
    ActorSystem(Main(), "examEvaluator")
  }

}
