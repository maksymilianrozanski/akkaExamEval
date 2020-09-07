package exams

import akka.NotUsed
import akka.actor.typed.{ActorSystem, Behavior, Terminated}
import akka.actor.typed.scaladsl.Behaviors

object Main {

  def apply(): Behavior[NotUsed] =
    Behaviors.setup { context =>
      val generator  = context.spawn(ExamDistributor(), "distributor")
      val student = context.spawn(StudentWaiting(), "student1")
      generator ! RequestExam(student)

      Behaviors.receiveSignal {
        case (_, Terminated(_)) => Behaviors.stopped
      }
    }

  def main(args: Array[String]): Unit = {
    ActorSystem(Main(), "examEvaluator")
  }

}
