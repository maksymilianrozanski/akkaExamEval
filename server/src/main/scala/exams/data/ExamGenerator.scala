package exams.data

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import exams.data.ExamRepository.{ExamRepository, TakeQuestionsSet, TakeQuestionsSetReply}
import exams.shared.data
import exams.shared.data.HttpRequests._
import exams.shared.data.{Answer, BlankQuestion, Question, TeachersExam}

import scala.util.Random

object ExamGenerator {
  case class ExamRequest(examId: ExamId, studentId: StudentId, maxQuestions: Int, setId: SetId)
  object ExamRequest {
    def fromStudentsRequest(examId: ExamId, request: StudentsRequest): ExamRequest =
      ExamRequest(examId, request.studentId, request.maxQuestions, request.setId)
  }

  case class ExamOutput(request: ExamRequest, teachersExam: Option[TeachersExam])
  type ExamRequestWithRef = (ExamRequest, ActorRef[ExamOutput])

  def sampleExam(id: ExamId): TeachersExam = {
    val q1Answers = List(Answer("yes"), Answer("no"))
    val q1 = Question(BlankQuestion("Do you like scala?", q1Answers), List(Answer("yes")))
    val q2Answers = List(Answer("no"), Answer("yes"))
    val q2 = Question(BlankQuestion("Do you like akka?", q2Answers), List(Answer("no")))
    TeachersExam(id, List(q1, q2))
  }

  sealed trait ExamGenerator
  //incoming msg from ExamDistributor
  final case class ReceivedExamRequest(examRequest: ExamRequest, replyTo: ActorRef[ExamOutput]) extends ExamGenerator
  final case class ReceivedSetFromRepo(set: TakeQuestionsSetReply) extends ExamGenerator

  case class State(requests: Set[ExamRequestWithRef])
  val emptyState: State = State(Set())

  def apply(repository: ActorRef[ExamRepository]): Behavior[ExamGenerator] = generator(repository)(emptyState)

  private sealed trait ExamGeneratorErrors
  private case class NotFoundResponse(examId: ExamId) extends ExamGeneratorErrors
  private final case class EmptyRepositoryResponse(request: ExamRequestWithRef) extends ExamGeneratorErrors

  def generator(repository: ActorRef[ExamRepository])(state: State): Behavior[ExamGenerator] =
    Behaviors.setup { context =>
      val responseMapper: ActorRef[TakeQuestionsSetReply] =
        context.messageAdapter(response => ReceivedSetFromRepo(response))

      Behaviors.receiveMessage {
        case ReceivedExamRequest(examRequest@ExamRequest(examId, _, _, setId), replyTo) =>
          val newState = state.copy(requests = state.requests + ((examRequest, replyTo)))
          repository ! TakeQuestionsSet(setId, examId, responseMapper)
          generator(repository)(newState)
        case ReceivedSetFromRepo((examId, optionSet)) =>

          val eitherExam = for (
            i <- persistedRequest(state)(examId);
            j <- eitherQuestionsSet(i)(optionSet)
          ) yield (i, createExam(j)(i._1))

          eitherExam match {
            case Right((request, teachersExam)) =>
              val replyTo = request._2
              replyTo ! ExamOutput(request._1, Some(teachersExam))
              generator(repository)(stateWithDropped(state)(examId))
            case Left(value) =>
              value match {
                case EmptyRepositoryResponse(request) =>
                  context.log.info("Repository returned no questions for examId:{} request", examId)
                  request._2 ! ExamOutput(request._1, None)
                  generator(repository)(stateWithDropped(state)(examId))
                case NotFoundResponse(examId) =>
                  context.log.error("Not found request corresponding to examId: {}! stopping the actor", examId)
                  Behaviors.stopped
              }
          }
      }
    }

  private def stateWithDropped(state: State)(idToDrop: ExamId) =
    state.copy(requests = state.requests.filterNot(_._1.examId == idToDrop))

  private def persistedRequest(state: State)(examId: ExamId) =
    state.requests.find(_._1.examId == examId) match {
      case Some(value) => Right(value)
      case None => Left(NotFoundResponse(examId))
    }

  private def eitherQuestionsSet(request: ExamRequestWithRef)(questionsSet: Option[QuestionsSet]): Either[ExamGeneratorErrors, QuestionsSet] =
    questionsSet match {
      case Some(value) => Right(value)
      case None => Left(EmptyRepositoryResponse(request))
    }

  private[data] def createExam(questionsSet: QuestionsSet)(examRequest: ExamRequest) = {
    data.TeachersExam(examRequest.examId, Random.shuffle(questionsSet.questions).take(examRequest.maxQuestions).toList)
  }
}
