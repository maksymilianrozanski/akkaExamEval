package exams


import java.util.logging.Logger

import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EffectBuilder, EventSourcedBehavior}
import exams.data.{ExamGenerator, TeachersExam}

object ExamDistributor {
  type Answers = List[List[String]]

  //commands
  sealed trait ExamDistributor
  final case class RequestExam(studentId: String, student: ActorRef[Student]) extends ExamDistributor
  final case class RequestExamEvaluationCompact(examId: String, answers: Answers) extends ExamDistributor

  //events
  sealed trait ExamDistributorEvents
  final case class ExamAdded(studentId: String, exam: TeachersExam) extends ExamDistributorEvents
  final case class ExamCompleted(examId: String, answers: Answers) extends ExamDistributorEvents

  type ExamId = String
  type StudentId = String
  case class PersistedExam(studentId: StudentId, exam: TeachersExam, answers: Option[Answers])
  case class ExamDistributorState(openExams: Map[ExamId, PersistedExam])
  val emptyState: ExamDistributorState = ExamDistributorState(Map())

  import exams.data.TeachersExam._

  def apply(evaluator: ActorRef[ExamEvaluator]): Behavior[ExamDistributor] = distributor(evaluator)

  def distributor(evaluator: ActorRef[ExamEvaluator]): Behavior[ExamDistributor] = Behaviors.setup[ExamDistributor](context => {
    EventSourcedBehavior[ExamDistributor, ExamDistributorEvents, ExamDistributorState](
      persistenceId = PersistenceId.ofUniqueId("examDistributor"),
      emptyState = emptyState,
      commandHandler = distributorCommandHandler(context, evaluator) _,
      eventHandler = distributorEventHandler
    )
  })

  def distributorCommandHandler(context: ActorContext[ExamDistributor], evaluator: ActorRef[ExamEvaluator])(state: ExamDistributorState, command: ExamDistributor): Effect[ExamDistributorEvents, ExamDistributorState] =
    command match {
      case request: RequestExam => onRequestExam(context)(ExamGenerator.sampleExam)(state, request)
      case RequestExamEvaluationCompact(examId, answers) =>
        // 1 - find exam of id in persisted
        state.openExams.get(examId) match {
          case Some(value) =>
            // 2 - persist answers
            //todo: check length of answers before persisting
            Effect.persist(ExamCompleted(examId, answers))
              .thenRun((s: ExamDistributorState) => {
                context.log.info("persisted ExamCompleted, id: {}", examId)
                // 3 - send answers to evaluator
                evaluator ! EvaluateAnswersCompact(examId, value.studentId, value.exam, answers)
              })
          case None =>
            context.log.info("Received RequestExamEvaluationCompact, not found examId {}", examId)
            Effect.none
        }
    }

  def onRequestExam[T >: RequestExam](context: ActorContext[T])(generator: String => TeachersExam)(state: ExamDistributorState, requestExam: RequestExam): EffectBuilder[ExamAdded, ExamDistributorState] = {
    val examId = state.openExams.size.toString
    val exam = generator(examId)
    Effect.persist(ExamAdded(requestExam.studentId, exam))
      .thenRun((s: ExamDistributorState) => {
        context.log.info("persisted examId {}", examId)
        requestExam.student ! GiveExamToStudent(exam)
      })
  }

  def distributorEventHandler(state: ExamDistributorState, event: ExamDistributorEvents): ExamDistributorState =
    event match {
      case examAdded@ExamAdded(_, _) => examAddedHandler(state, examAdded)
      case ExamCompleted(examId, answers) =>
        val currentWithoutAnswers = state.openExams.get(examId)
        currentWithoutAnswers match {
          case Some(value) => state.copy(openExams = state.openExams.updated(examId, value.copy(answers = Some(answers))))
          case None =>
            println(s"distributorEventHandler: Not found examId $examId !")
            state
        }
    }

  def examAddedHandler(state: ExamDistributorState, event: ExamAdded): ExamDistributorState =
    state.copy(openExams = state.openExams.updated(event.exam.examId, PersistedExam(event.studentId, event.exam, None)))
}
