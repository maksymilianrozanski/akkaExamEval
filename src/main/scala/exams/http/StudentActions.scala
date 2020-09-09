package exams.http

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors

object StudentActions {

  sealed trait Command
  final case class TestCommand(replyTo: ActorRef[ActionPerformed]) extends Command

  final case class ActionPerformed(description: String)

  def apply(): Behavior[Command] = registry()

  def registry(): Behavior[Command] = {
    Behaviors.setup(context =>
      Behaviors.receiveMessage {
        case TestCommand(replyTo) =>
          context.log.info("received test command")
          replyTo ! ActionPerformed("test action performed!")
          Behaviors.same
      })
  }
}
