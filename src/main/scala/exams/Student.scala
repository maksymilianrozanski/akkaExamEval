package exams

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import exams.data.{CompletedExam, EmptyExam}

sealed trait StudentWaiting
final case class StudentReceivedExam(emptyExam: EmptyExam, examEvaluator: ActorRef[ReceivedAnswers]) extends StudentWaiting

object StudentWaiting {

  def apply(): Behavior[StudentWaiting] = studentWaiting()

  def studentWaiting(): Behavior[StudentWaiting] = {
    Behaviors.setup(context =>
      Behaviors.receiveMessage {
        case StudentReceivedExam(emptyExam, examEvaluator) =>
          val waitingForResult = context.spawn(StudentWaiting2(), "student-waiting")
          examEvaluator ! ReceivedAnswers(waitingForResult, CompletedExam(emptyExam.questions))
          Behaviors.ignore
      })
  }
}

sealed trait StudentWaiting2
final case class StudentWaitingForResult(result: Double) extends StudentWaiting2

object StudentWaiting2 {
  def apply(): Behavior[StudentWaiting2] =
    Behaviors.setup(context =>
      Behaviors.receiveMessage {
        case StudentWaitingForResult(result) =>
          context.log.info("received result {} !", result)
          Behaviors.stopped
      }
    )
}


