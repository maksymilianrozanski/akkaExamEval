package exams.data

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EffectBuilder, EventSourcedBehavior}

object ExamRepository {

  def apply(): Behavior[ExamRepository] = examRepository()

  sealed trait ExamRepository
  final case class AddQuestionsSet(questionsSet: QuestionsSet) extends ExamRepository

  sealed trait ExamRepositoryEvents
  final case class QuestionsSetAdded(questions: QuestionsSet) extends ExamRepositoryEvents

  val emptyState: ExamRepositoryState = ExamRepositoryState(List())
  case class ExamRepositoryState(questions: List[QuestionsSet])

  case class QuestionsSet(setId: String, description: String, questions: Set[Question])

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
    }

  def onAddQuestionsSet(context: ActorContext[ExamRepository])(state: ExamRepositoryState, command: AddQuestionsSet): EffectBuilder[QuestionsSetAdded, ExamRepositoryState] = {
    if (state.questions.exists(_.setId == command.questionsSet.setId))
      Effect.none.thenRun { s: ExamRepositoryState =>
        context.log.info("Questions set with provided id {} already exist - not saving.", command.questionsSet.setId)
      } else
      Effect.persist(QuestionsSetAdded(command.questionsSet)).thenRun((s: ExamRepositoryState) =>
        context.log.info("Persisted {} questions with id: {}", command.questionsSet.questions.size,
          command.questionsSet.setId))
  }

  def eventHandler(state: ExamRepositoryState, events: ExamRepositoryEvents): ExamRepositoryState =
    events match {
      case QuestionsSetAdded(questions) => state.copy(questions = state.questions :+ questions)
    }
}
