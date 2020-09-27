package exams


import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EffectBuilder, EventSourcedBehavior}
import exams.data.ExamGenerator.ExamGenerator
import exams.data.{Answer, ExamGenerator, TeachersExam}
import exams.evaluator.ExamEvaluator.{EvaluateAnswers, ExamEvaluator}

object ExamDistributor {

  type Answers = List[List[Answer]]

  //commands
  sealed trait ExamDistributor
  final case class RequestExam(studentId: String, student: ActorRef[Student]) extends ExamDistributor
  final case class RequestExamEvaluation(examId: String, answers: Answers) extends ExamDistributor

  //events
  sealed trait ExamDistributorEvents
  final case class ExamAdded(studentId: String, exam: TeachersExam) extends ExamDistributorEvents
  final case class ExamCompleted(examId: String, answers: Answers) extends ExamDistributorEvents

  type ExamId = String
  type StudentId = String
  case class PersistedExam(studentId: StudentId, exam: TeachersExam)
  case class PersistedAnswers(answers: Answers)
  case class ExamDistributorState(exams: Map[ExamId, PersistedExam], answers: Map[ExamId, PersistedAnswers])
  val emptyState: ExamDistributorState = ExamDistributorState(Map(), Map())

  import exams.data.TeachersExam._

  case class ActorsPack(evaluator: ActorRef[ExamEvaluator], generator: ActorRef[ExamGenerator])
  def apply(evaluator: ActorRef[ExamEvaluator], actorsPack: ActorsPack): Behavior[ExamDistributor] = distributor(actorsPack)

  def distributor(actorsPack: ActorsPack): Behavior[ExamDistributor] = Behaviors.setup[ExamDistributor](context => {
    EventSourcedBehavior[ExamDistributor, ExamDistributorEvents, ExamDistributorState](
      persistenceId = PersistenceId.ofUniqueId("examDistributor"),
      emptyState = emptyState,
      commandHandler = distributorCommandHandler(context, actorsPack) _,
      eventHandler = distributorEventHandler
    )
  })

  def distributorCommandHandler(context: ActorContext[ExamDistributor], actors: ActorsPack)(state: ExamDistributorState, command: ExamDistributor): Effect[ExamDistributorEvents, ExamDistributorState] =
    command match {
      case request: RequestExam => onRequestExam(context)(ExamGenerator.sampleExam)(state, request)
      case request: RequestExamEvaluation => onRequestExamEvaluation(context, actors.evaluator)(state, request)
    }

  def onRequestExam[T >: RequestExam](context: ActorContext[T])(generator: String => TeachersExam)(state: ExamDistributorState, requestExam: RequestExam): EffectBuilder[ExamAdded, ExamDistributorState] = {
    val examId = state.exams.size.toString
    val exam = generator(examId)
    Effect.persist(ExamAdded(requestExam.studentId, exam))
      .thenRun((s: ExamDistributorState) => {
        context.log.info("persisted examId {}", examId)
        requestExam.student ! GiveExamToStudent(exam)
      })
  }

  def onRequestExamEvaluation[T >: RequestExamEvaluation](context: ActorContext[T], evaluator: ActorRef[ExamEvaluator])(state: ExamDistributorState, command: RequestExamEvaluation): EffectBuilder[ExamCompleted, ExamDistributorState] = {
    command match {
      case RequestExamEvaluation(examId, answers) =>
        // 1 - find exam of id in persisted
        state.exams.get(examId) match {
          case Some(value) =>
            if (answersLengthIsValid(value, answers))
            // 2 - persist answers
              Effect.persist(ExamCompleted(examId, answers))
                .thenRun((s: ExamDistributorState) => {
                  context.log.info("persisted ExamCompleted, id: {}", examId)
                  // 3 - send answers to evaluator
                  evaluator ! EvaluateAnswers(value.studentId, value.exam, answers)
                })
            else Effect.none.thenRun(_ =>
              context.log.info("Answer's length:({}) isn't equal to expected: ({})", answers.length, value.exam.questions.length)
            )
          case None =>
            context.log.info("Received RequestExamEvaluation, not found examId {}", examId)
            Effect.none
        }
    }
  }

  private def answersLengthIsValid(persistedExam: PersistedExam, answers: Answers) =
    persistedExam.exam.questions.lengthCompare(answers) == 0

  def distributorEventHandler(state: ExamDistributorState, event: ExamDistributorEvents): ExamDistributorState =
    event match {
      case examAdded: ExamAdded => examAddedHandler(state, examAdded)
      case examCompleted: ExamCompleted => examCompletedHandler(state, examCompleted)
    }

  def examAddedHandler(state: ExamDistributorState, event: ExamAdded): ExamDistributorState =
    state.copy(exams = state.exams.updated(event.exam.examId, PersistedExam(event.studentId, event.exam)))

  def examCompletedHandler(state: ExamDistributorState, event: ExamCompleted): ExamDistributorState =
    state.copy(answers = state.answers.updated(event.examId, PersistedAnswers(event.answers)))

}
