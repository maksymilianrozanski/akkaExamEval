package exams

import akka.NotUsed
import akka.actor.typed.{ActorSystem, Behavior, Terminated}
import akka.actor.typed.scaladsl.Behaviors

object Main {

  def apply(): Behavior[NotUsed] =
    Behaviors.setup { context =>
      val generator = context.spawn(ExamDistributor(), "distributor")
      val student1 = context.spawn(Student(), "student1")
      val student2 = context.spawn(Student(), "student2")
      generator ! RequestExam(student1)
      generator ! RequestExam(student2)

      Behaviors.receiveSignal {
        case (_, Terminated(_)) => Behaviors.stopped
      }
    }

  def main(args: Array[String]): Unit = {
    ActorSystem(Main(), "examEvaluator")
  }

}
