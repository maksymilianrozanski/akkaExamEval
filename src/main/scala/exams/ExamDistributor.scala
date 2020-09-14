package exams

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import exams.data.{ExamGenerator, StudentsExam}

sealed trait ExamDistributor
final case class RequestExam(student: ActorRef[Student]) extends ExamDistributor

object ExamDistributor {

  import exams.data.TeachersExam._

  def apply(): Behavior[ExamDistributor] = distributor

  def distributor: Behavior[ExamDistributor] = Behaviors.setup[ExamDistributor](context => {
    Behaviors.receiveMessage[ExamDistributor] {
      case RequestExam(student) =>
        context.log.info(s"Receive RequestExam: ${RequestExam(student)}")
        val evaluator = context.spawnAnonymous(ExamEvaluator(ExamGenerator.sampleExam()))
        val exam: StudentsExam = ExamGenerator.sampleExam()
        student ! GiveExamToStudent(exam, evaluator)
        Behaviors.same
    }
  })
}
