package exams

import akka.NotUsed
import akka.actor.typed.{ActorSystem, Behavior, Terminated}
import akka.actor.typed.scaladsl.Behaviors

object Main {

  def apply(): Behavior[NotUsed] =
    Behaviors.setup { context =>
      val examEvaluator = context.spawn(ExamEvaluator(), "evaluator")
      val studentA = context.spawn(Student(), "studentA")
      val studentB = context.spawn(Student(), "studentB")
      val studentC = context.spawn(Student(), "studentC")

      context.watch(examEvaluator)

      examEvaluator ! RequestExam(studentA)
      examEvaluator ! RequestExam(studentB)
      examEvaluator ! RequestExam(studentC)

      Behaviors.receiveSignal {
        case (_, Terminated(_)) => Behaviors.stopped
      }
    }

  def main(args: Array[String]): Unit = {
    ActorSystem(Main(), "examEvaluator")
  }

}
