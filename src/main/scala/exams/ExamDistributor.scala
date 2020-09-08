package exams

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import exams.data.ExamGenerator

sealed trait ExamDistributor
final case class RequestExam(student: ActorRef[Student]) extends ExamDistributor

object ExamDistributor {

  def apply(): Behavior[ExamDistributor] = distributor

  def distributor: Behavior[ExamDistributor] = Behaviors.setup[ExamDistributor](context => {
    Behaviors.receiveMessage[ExamDistributor] {
      case RequestExam(student) =>
        val evaluator = context.spawn(ExamEvaluatorWaiting(ExamGenerator.sampleExam()), "evaluator")
        student ! GiveExamToStudent(ExamGenerator.sampleExam(), evaluator)
        Behaviors.same
    }
  })
}
