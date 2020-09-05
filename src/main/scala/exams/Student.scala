package exams

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import exams.data.{CompletedExam, EmptyExam}

object Student {

  def apply(): Behavior[Student] = student()

  def student(): Behavior[Student] = {
    Behaviors.setup { context =>
      Behaviors.receiveMessage {
        case ReceivedExam(emptyExam, evaluator) =>
          evaluator ! EvaluateExam(context.self, CompletedExam())
          Behaviors.same
        case ExamResult(score) =>
          if (score > 0.7)
            context.log.info("I'm very happy!")
          else {
            context.log.info("I'm not very happy")
          }
          Behaviors.stopped
      }
    }
  }
}

sealed trait Student
final case class ReceivedExam(emptyExam: EmptyExam, evaluator: ActorRef[ExamEvaluator]) extends Student
final case class ExamResult(score: Double) extends Student
