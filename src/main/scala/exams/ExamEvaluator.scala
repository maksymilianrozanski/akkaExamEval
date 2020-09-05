package exams

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors

sealed trait ExamEvaluator
final case class RequestExam(student: ActorRef[Student]) extends ExamEvaluator
final case class EvaluateExam(student: ActorRef[Student], completedExam: CompletedExam) extends ExamEvaluator

object ExamEvaluator {

  def apply(): Behavior[ExamEvaluator] = evaluator()

  def evaluator(): Behavior[ExamEvaluator] = {
    Behaviors.setup(context =>
      Behaviors.receiveMessage {
        case RequestExam(student) =>
          student ! ReceivedExam(EmptyExam(), context.self)
          Behaviors.same
        case EvaluateExam(student, completedExam) =>
          student ! ExamResult(0.78)
          Behaviors.same
      })
  }
}

sealed trait Student
final case class ReceivedExam(emptyExam: EmptyExam, evaluator: ActorRef[ExamEvaluator]) extends Student
final case class ExamResult(score: Double) extends Student

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

//data
sealed trait Exam
final case class EmptyExam()
final case class CompletedExam()
