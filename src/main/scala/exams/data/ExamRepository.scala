package exams.data

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EffectBuilder, EventSourcedBehavior}
import exams.JsonSerializable
import exams.distributor.ExamDistributor.ExamId

object ExamRepository {

  type SetId = String
  type TakeQuestionsSetReply = (ExamId, Option[QuestionsSet])

  def apply(): Behavior[ExamRepository] = examRepository()

  sealed trait ExamRepository extends JsonSerializable
  final case class AddQuestionsSet(questionsSet: QuestionsSet) extends ExamRepository
  final case class TakeQuestionsSet(setId: SetId, examId: ExamId, replyTo: ActorRef[TakeQuestionsSetReply]) extends ExamRepository

  sealed trait ExamRepositoryEvents  extends JsonSerializable
  final case class QuestionsSetAdded(questions: QuestionsSet) extends ExamRepositoryEvents

  val emptyState: ExamRepositoryState = ExamRepositoryState(List())
  case class ExamRepositoryState(questions: List[QuestionsSet])  extends JsonSerializable

  case class QuestionsSet(setId: SetId, description: String, questions: Set[Question])

  def examRepository(): Behavior[ExamRepository] = Behaviors.setup[ExamRepository] { context =>
    EventSourcedBehavior[ExamRepository, ExamRepositoryEvents, ExamRepositoryState](
      persistenceId = PersistenceId.ofUniqueId("examRepository"),
      emptyState = emptyState,
      commandHandler = commandHandler(context) _,
      eventHandler = eventHandler
    )
  }

  def commandHandler(context: ActorContext[ExamRepository])(state: ExamRepositoryState, command: ExamRepository): Effect[ExamRepositoryEvents, ExamRepositoryState] =
    command match {
      case addQuestionsSet: AddQuestionsSet => onAddQuestionsSet(context)(state, addQuestionsSet)
      case takeQuestionsSet: TakeQuestionsSet => onTakeQuestionsSet(context)(state, takeQuestionsSet)
    }

  def onAddQuestionsSet(context: ActorContext[ExamRepository])(state: ExamRepositoryState, command: AddQuestionsSet)
  : EffectBuilder[QuestionsSetAdded, ExamRepositoryState] =
    questionsNonEmpty(command).flatMap(idNotExists(state)(_))
    match {
      case Right(command) =>
        Effect.persist(QuestionsSetAdded(command.questionsSet)).thenRun((s: ExamRepositoryState) =>
          context.log.info("Persisted {} questions with id: {}", command.questionsSet.questions.size,
            command.questionsSet.setId))
      case Left(error) =>
        Effect.none.thenRun {
          _: ExamRepositoryState =>
            context.log.info(error)
        }
    }

  private def onTakeQuestionsSet(context: ActorContext[ExamRepository])(state: ExamRepositoryState, command: TakeQuestionsSet) = {
    Effect.none.thenReply(command.replyTo) { (s: ExamRepositoryState) =>
      val result = s.questions.find(_.setId == command.setId)
      result match {
        case Some(_) =>
          context.log.info("Found set with setId: {}", command.setId)
        case None =>
          context.log.info("Set with setId: {} not found", command.setId)
      }
      (command.examId, result)
    }
  }

  private def idNotExists(state: ExamRepositoryState)(command: AddQuestionsSet) =
    if (!state.questions.exists(_.setId == command.questionsSet.setId)) Right(command)
    else Left(s"set with id: ${command.questionsSet.setId} already exists")

  private def questionsNonEmpty(command: AddQuestionsSet) =
    if (command.questionsSet.questions.nonEmpty) Right(command)
    else Left(s"questions set ${command.questionsSet.setId} is empty")

  def eventHandler(state: ExamRepositoryState, events: ExamRepositoryEvents): ExamRepositoryState =
    events match {
      case QuestionsSetAdded(questions) => state.copy(questions = state.questions :+ questions)
    }
}
