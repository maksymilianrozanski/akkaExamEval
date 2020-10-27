package exams.evaluator

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EffectBuilder, EventSourcedBehavior}
import exams.JsonSerializable
import exams.distributor.ExamDistributor.{Answers, ExamId}
import exams.http.StudentActions
import exams.http.StudentActions.DisplayedToStudent
import exams.shared.data.TeachersExam
import exams.student.{GiveResultToStudent, Student}

object ExamEvaluator {

  sealed trait ExamEvaluator extends JsonSerializable
  final case class EvaluateAnswers(studentId: String, teachersExam: TeachersExam, answers: Answers, replyTo: Option[ActorRef[Student]]) extends ExamEvaluator
  final case class RequestResults(replyTo: ActorRef[List[ExamResult]]) extends ExamEvaluator
  final case class RequestSingleResult(examId: ExamId, replyTo: ActorRef[Option[ExamResult]]) extends ExamEvaluator

  sealed trait ExamEvaluatorEvents extends JsonSerializable
  final case class ExamEvaluated(examResult: ExamResult) extends ExamEvaluatorEvents

  val emptyState: ExamEvaluatorState = ExamEvaluatorState(List())
  final case class ExamEvaluatorState(results: List[ExamResult]) extends JsonSerializable
  case class ExamResult(examId: String, studentId: String, result: Double)

  def apply(): Behavior[ExamEvaluator] = evaluator()

  private[evaluator] def evaluator(): Behavior[ExamEvaluator] = Behaviors.setup[ExamEvaluator](context => {
    EventSourcedBehavior[ExamEvaluator, ExamEvaluatorEvents, ExamEvaluatorState](
      persistenceId = PersistenceId.ofUniqueId("examEvaluator"),
      emptyState = emptyState,
      commandHandler = commandHandler(context) _,
      eventHandler = eventHandler
    )
  })

  private[evaluator]
  def commandHandler(context: ActorContext[ExamEvaluator])(state: ExamEvaluatorState, command: ExamEvaluator)
  : Effect[ExamEvaluated, ExamEvaluatorState] =
    command match {
      case evaluateAnswers: EvaluateAnswers => onEvaluateExamCommand(context)(state, evaluateAnswers)
      case requestResults: RequestResults => onRequestResultsCommand(context)(requestResults)
      case requestSingleResult: RequestSingleResult => onRequestSingleResultCommand(context)(requestSingleResult)
    }

  private[evaluator] def onRequestResultsCommand[T >: RequestResults](context: ActorContext[T])(command: RequestResults) =
    Effect.none.thenReply(command.replyTo)((s: ExamEvaluatorState) => {
      val results = s.results
      context.log.info("Sending back all results {}", results)
      results
    })

  private[evaluator] def onRequestSingleResultCommand[T >: RequestSingleResult](context: ActorContext[T])(command: RequestSingleResult) =
    Effect.none.thenReply(command.replyTo) { s: ExamEvaluatorState =>
      val result = s.results.find(_.examId == command.examId)
      result match {
        case result@Some(examResult) =>
          context.log.info(s"Found exam result: $examResult")
          result
        case None =>
          context.log.info(s"Not found result with requested examId(${command.examId})")
          None
      }
    }

  private[evaluator] def eventHandler(state: ExamEvaluatorState, event: ExamEvaluatorEvents): ExamEvaluatorState =
    event match {
      case examEvaluated: ExamEvaluated => onExamEvaluatedEvent(state, examEvaluated)
    }

  private[evaluator] def onEvaluateExamCommand[T >: EvaluateAnswers](context: ActorContext[T])(state: ExamEvaluatorState, command: EvaluateAnswers)
  : EffectBuilder[ExamEvaluated, ExamEvaluatorState] =
    command match {
      case EvaluateAnswers(studentId, teachersExam@TeachersExam(examId, _), answers, replyTo) =>
        context.log.info("Received exam evaluation request")
        val examScore = percentOfCorrectAnswers(teachersExam, answers)
        val examResult = ExamResult(examId, studentId, examScore)
        replyTo.foreach(_ ! GiveResultToStudent(examResult))
        context.log.info("exam {} of student {} result: {}", examId, studentId, examScore)
        Effect.persist(ExamEvaluated(examResult))
          .thenRun((s: ExamEvaluatorState) =>
            context.log.info("persisted exam result {}", examId))
    }

  private[evaluator] def onExamEvaluatedEvent(state: ExamEvaluatorState, event: ExamEvaluated): ExamEvaluatorState =
    ExamEvaluatorState(state.results :+ event.examResult)

  private[evaluator] def percentOfCorrectAnswers(teachersExam: TeachersExam, answers: Answers) = {
    val validAnswers = teachersExam.questions.map(_.correctAnswers).map(_.map(_.toString))
    assert(validAnswers.nonEmpty, "exam should contain at least one question")
    assert(answers.length == validAnswers.length, "length of student's answers should be equal to list of valid answers")
    percentOfPoints(validAnswers, answers)
  }

  private[evaluator] def percentOfPoints[T](validAnswers: List[T], studentsAnswers: List[T]) = {
    val points = validAnswers.zip(studentsAnswers)
      .map(pair => if (pair._1.toString == pair._2.toString) 1 else 0)
      .sum
    points.toDouble / validAnswers.length.toDouble
  }
}
