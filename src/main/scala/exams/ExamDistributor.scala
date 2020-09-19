package exams

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import exams.data.{CompletedExam, ExamGenerator, StudentsExam}

sealed trait ExamDistributor
final case class RequestExam(student: ActorRef[Student]) extends ExamDistributor
final case class RequestExamEvaluation(completedExam: CompletedExam) extends ExamDistributor

object ExamDistributor {

  import exams.data.TeachersExam._

  def apply(evaluator: ActorRef[ExamEvaluator]): Behavior[ExamDistributor] = distributor(evaluator)

  def distributor(evaluator: ActorRef[ExamEvaluator]): Behavior[ExamDistributor] = Behaviors.setup[ExamDistributor](context => {
    Behaviors.receiveMessage[ExamDistributor] {
      case RequestExam(student) =>
        context.log.info(s"Receive RequestExam: ${RequestExam(student)}")
        val exam: StudentsExam = ExamGenerator.sampleExam()
        student ! GiveExamToStudent(exam)
        Behaviors.same
      case RequestExamEvaluation(completedExam) =>
        context.log.info(s"Received exam evaluation request $completedExam")
        //todo: send persisted teacher's exam to evaluator instead of generating new
        val exam = ExamGenerator.sampleExam()
        evaluator ! EvaluateAnswers(exam, completedExam)
        Behaviors.same
    }
  })
}
