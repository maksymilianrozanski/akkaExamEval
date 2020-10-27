package exams.distributor

import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EffectBuilder, EventSourcedBehavior, ReplyEffect}
import exams.JsonSerializable
import exams.data.ExamGenerator.{ExamGenerator, ExamOutput}
import exams.data._
import exams.evaluator.ExamEvaluator.{EvaluateAnswers, ExamEvaluator}
import exams.http.StudentActions
import exams.shared.data
import exams.shared.data.HttpRequests.StudentsRequest
import exams.shared.data.{Answer, ExamRequest, TeachersExam}
import exams.student.{GeneratingExamFailed, GiveExamToStudent, Student}

object ExamDistributor {

  type Answers = List[List[Answer]]
  type ExamId = String
  type StudentId = String

  //commands
  sealed trait ExamDistributor extends JsonSerializable
  /**
   * incoming message - Requests generating new exam
   *
   * @param studentsRequest request exam with n questions from specific set
   * @param student         ActorRef to actor who requested the exam
   */
  final case class RequestExam(studentsRequest: StudentsRequest, student: ActorRef[Student]) extends ExamDistributor
  final case class RequestExamEvaluation(examId: String, answers: Answers, replyTo: Option[ActorRef[Student]]) extends ExamDistributor
  private[exams] case class ReceivedGeneratedExam(exam: ExamOutput) extends ExamDistributor

  //events
  sealed trait ExamDistributorEvents extends JsonSerializable
  final case class ExamRequested(examId: ExamId, student: ActorRef[Student]) extends ExamDistributorEvents
  final case class ExamRequestRemoved(examId: ExamId) extends ExamDistributorEvents
  final case class ExamAdded(studentId: String, exam: TeachersExam) extends ExamDistributorEvents
  final case class ExamCompleted(examId: String, answers: Answers) extends ExamDistributorEvents

  case class PersistedExam(studentId: StudentId, exam: TeachersExam)
  case class PersistedAnswers(answers: Answers)
  case class ExamDistributorState(exams: Map[ExamId, PersistedExam], answers: Map[ExamId, PersistedAnswers],
                                  requests: Map[ExamId, ActorRef[Student]], lastExamId: Int) extends JsonSerializable
  val emptyState: ExamDistributorState = ExamDistributorState(Map(), Map(), Map(), 0)

  import exams.shared.data.TeachersExam._

  case class ActorsPack(evaluator: ActorRef[ExamEvaluator], generator: ActorRef[ExamGenerator])

  def apply(evaluator: ActorRef[ExamEvaluator], actorsPack: ActorsPack): Behavior[ExamDistributor] = distributor(actorsPack)

  def distributor(actorsPack: ActorsPack): Behavior[ExamDistributor] = Behaviors.setup[ExamDistributor](context => {
    val messageAdapter: ActorRef[ExamOutput] =
      context.messageAdapter(response => ReceivedGeneratedExam(response))

    EventSourcedBehavior[ExamDistributor, ExamDistributorEvents, ExamDistributorState](
      persistenceId = PersistenceId.ofUniqueId("examDistributor"),
      emptyState = emptyState,
      commandHandler = distributorCommandHandler(context, actorsPack, messageAdapter) _,
      eventHandler = distributorEventHandler
    )
  })

  def distributorCommandHandler
  (context: ActorContext[ExamDistributor], actors: ActorsPack, messageAdapter: ActorRef[ExamOutput])
  (state: ExamDistributorState, command: ExamDistributor): Effect[ExamDistributorEvents, ExamDistributorState] =
    command match {
      case request: RequestExam => onRequestExam(context)(actors.generator, messageAdapter)(state, request)
      case request: RequestExamEvaluation => onRequestExamEvaluation(context, actors.evaluator)(state, request)
      case message: ReceivedGeneratedExam => onReceivingGeneratedExam(context)(state, message)
    }

  def onRequestExam[T >: RequestExam]
  (context: ActorContext[T])(generator: ActorRef[ExamGenerator], messageAdapter: ActorRef[ExamOutput])
  (state: ExamDistributorState, command: RequestExam): ReplyEffect[ExamRequested, ExamDistributorState] = {
    val nextExamId = (state.lastExamId + 1).toString
    Effect.persist(ExamRequested(nextExamId, command.student))
      .thenReply(generator) {
        state: ExamDistributorState =>
          context.log.info("persisted ExamRequested, id: {}", state.lastExamId)
          val examRequest = data.ExamRequest(nextExamId, command.studentsRequest.studentId,
            command.studentsRequest.maxQuestions, command.studentsRequest.setId)
          val messageToGenerator = ExamGenerator.ReceivedExamRequest(examRequest, messageAdapter)
          messageToGenerator
      }
  }

  def onReceivingGeneratedExam[T >: ReceivedGeneratedExam]
  (context: ActorContext[T])(state: ExamDistributorState, message: ReceivedGeneratedExam)
  : Effect[ExamDistributorEvents, ExamDistributorState] = {
    message match {
      case ReceivedGeneratedExam(ExamOutput(ExamRequest(examId, studentId, _, _), maybeExam)) =>
        val studentAndExam = (state.requests.get(examId), maybeExam)
        studentAndExam match {
          case (Some(studentRef), Some(exam)) =>
            Effect.persist(ExamAdded(studentId, exam)).thenReply(studentRef) {
              (_: ExamDistributorState) =>
                context.log.info("Sending generated exam(examId: {}) to student", examId)
                GiveExamToStudent(exam)
            }
          case (Some(studentRef), None) =>
            Effect.persist(ExamRequestRemoved(examId)).thenReply(studentRef) {
              (_: ExamDistributorState) =>
                context.log.info("Generating exam not successful")
                GeneratingExamFailed
            }
          case (None, _) =>
            //student ref not found - corrupted state?
            context.log.error("Student ref corresponding to ExamId: {} not found!", examId)
            //todo: add test case
            Effect.stop()
        }
    }
  }

  def onRequestExamEvaluation[T >: RequestExamEvaluation]
  (context: ActorContext[T], evaluator: ActorRef[ExamEvaluator])
  (state: ExamDistributorState, command: RequestExamEvaluation): EffectBuilder[ExamCompleted, ExamDistributorState] =
    command match {
      case request@RequestExamEvaluation(examId, answers, replyTo) =>
        examExists(request, state)
          .flatMap(answersLengthIsValid)
          .flatMap(examAlreadyNotEvaluated) match {
          case Right(ShouldSendToEvaluation(r)) =>
            Effect.persist(ExamCompleted(examId, answers))
              .thenRun((s: ExamDistributorState) => {
                context.log.info("persisted ExamCompleted, id: {}", examId)
                evaluator ! EvaluateAnswers(r._2.studentId, r._2.exam, r._1.answers, replyTo)
              })
          case Left(error) =>
            Effect.none.thenRun(_ => error match {
              case ExamNotFound(examId) => context.log.info("onRequestExamEvaluation, examId: {} not found.", examId)
              case AnswersLengthNotEqual(examId) => context.log.info("onRequestExamEvaluation, examId: {}, Answer's length isn't equal to expected", examId)
              case AlreadyEvaluated(examId) => context.log.info("onRequestExamEvaluation, examId: {}, exam already sent to evaluation", examId)
            })
        }
    }

  sealed trait ExamValidationError
  final case class ExamNotFound(examId: ExamId) extends ExamValidationError
  final case class AnswersLengthNotEqual(examId: ExamId) extends ExamValidationError
  final case class AlreadyEvaluated(examId: ExamId) extends ExamValidationError
  final case class ShouldSendToEvaluation(request: (RequestExamEvaluation, PersistedExam, ExamDistributorState))

  private def examExists(request: RequestExamEvaluation, state: ExamDistributorState) =
    state.exams.get(request.examId) match {
      case Some(persistedExam) => Right((request, persistedExam, state))
      case None => Left(ExamNotFound(request.examId))
    }

  private def answersLengthIsValid(request: (RequestExamEvaluation, PersistedExam, ExamDistributorState)) =
    if (request._2.exam.questions.lengthCompare(request._1.answers) == 0) Right(request)
    else Left(AnswersLengthNotEqual(request._1.examId))

  private def examAlreadyNotEvaluated(request: (RequestExamEvaluation, PersistedExam, ExamDistributorState)) =
    if (request._3.answers.exists(_._1 == request._2.exam.examId)) Left(AlreadyEvaluated(request._1.examId))
    else Right(ShouldSendToEvaluation(request))

  def distributorEventHandler(state: ExamDistributorState, event: ExamDistributorEvents): ExamDistributorState =
    event match {
      case examRequested: ExamRequested => onExamRequestedHandler(state, examRequested)
      case requestRemoved: ExamRequestRemoved => onExamRequestRemovedHandler(state, requestRemoved)
      case examAdded: ExamAdded => examAddedHandler(state, examAdded)
      case examCompleted: ExamCompleted => examCompletedHandler(state, examCompleted)
    }

  def onExamRequestedHandler(state: ExamDistributorState, event: ExamRequested): ExamDistributorState =
    state.copy(requests = state.requests.updated(event.examId, event.student), lastExamId = state.lastExamId + 1)

  def onExamRequestRemovedHandler(state: ExamDistributorState, event: ExamRequestRemoved): ExamDistributorState =
    state.copy(requests = state.requests.filterNot(_._1 == event.examId))

  def examAddedHandler(state: ExamDistributorState, event: ExamAdded): ExamDistributorState =
    state.copy(
      exams = state.exams.updated(event.exam.examId, PersistedExam(event.studentId, event.exam)),
      requests = state.requests.filterNot(_._1 == event.exam.examId))

  //todo: remove completed exam from ExamDistributorState exams on ExamCompleted
  def examCompletedHandler(state: ExamDistributorState, event: ExamCompleted): ExamDistributorState =
    state.copy(answers = state.answers.updated(event.examId, PersistedAnswers(event.answers)))
}
