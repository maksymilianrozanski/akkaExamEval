package exams

import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior}
import exams.data.{CompletedExam, ExamGenerator, StudentsExam, TeachersExam}


//commands
sealed trait ExamDistributor
final case class RequestExam(studentId: String, student: ActorRef[Student]) extends ExamDistributor
final case class RequestExamEvaluation(completedExam: CompletedExam) extends ExamDistributor
final case class RequestExamEvaluationCompact(examId: String, answers: List[List[String]]) extends ExamDistributor

//events
sealed trait ExamDistributorEvents
final case class ExamAdded(examId: String, studentId: String, exam: TeachersExam) extends ExamDistributorEvents


object ExamDistributor {

  type ExamId = String
  type StudentId = String
  case class ExamDistributorState(openExams: Map[ExamId, (StudentId, TeachersExam)])

  import exams.data.TeachersExam._

  def apply(evaluator: ActorRef[ExamEvaluator]): Behavior[ExamDistributor] = distributor(evaluator)

  def distributor(evaluator: ActorRef[ExamEvaluator]): Behavior[ExamDistributor] = Behaviors.setup[ExamDistributor](context => {
    EventSourcedBehavior[ExamDistributor, ExamDistributorEvents, ExamDistributorState](
      persistenceId = PersistenceId.ofUniqueId("examDistributor"),
      emptyState = ExamDistributorState(Map()),
      commandHandler = distributorCommandHandler(context) _,
      eventHandler = distributorEventHandler
    )

//    Behaviors.receiveMessage[ExamDistributor] {
//      case request@RequestExam(studentId, student) =>
//        context.log.info(s"Receive RequestExam: ${request}")
//        val exam: StudentsExam = ExamGenerator.sampleExam()
//        student ! GiveExamToStudent(exam)
//        Behaviors.same
//      case RequestExamEvaluation(completedExam) =>
//        context.log.info(s"Received exam evaluation request $completedExam")
//        //todo: send persisted teacher's exam to evaluator instead of generating new
//        val exam = ExamGenerator.sampleExam()
//        evaluator ! EvaluateAnswers(exam, completedExam)
//        Behaviors.same
//      case request@RequestExamEvaluationCompact(examId, answers) =>
//        context.log.info("Received RequestExamEvaluationCompact {}, not implemented yet", request)
//        //todo: not implemented
//        Behaviors.same
//    }
  })

  def distributorCommandHandler(context: ActorContext[ExamDistributor])(state: ExamDistributorState, command: ExamDistributor): Effect[ExamDistributorEvents, ExamDistributorState] =
    command match {
      case RequestExam(studentId, studentRef) =>
        val exam: TeachersExam = ExamGenerator.sampleExam()
        val examId = state.openExams.size.toString
        Effect.persist(ExamAdded(examId, studentId, exam))
          .thenRun((s: ExamDistributorState) => {
            context.log.info("persisted examId: {}", examId)
            studentRef ! GiveExamToStudent(exam)
          })
      case RequestExamEvaluation(completedExam) => ???
      case RequestExamEvaluationCompact(examId, answers) => ???
    }

  def distributorEventHandler(state: ExamDistributorState, event: ExamDistributorEvents): ExamDistributorState =
    event match {
      case ExamAdded(examId, studentId, exam) => state.copy(openExams = state.openExams.updated(examId, (studentId, exam)))
    }

}
