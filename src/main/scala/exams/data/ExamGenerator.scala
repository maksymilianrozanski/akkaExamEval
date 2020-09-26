package exams.data

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import exams.ExamDistributor.ExamId
import exams.data.ExamRepository.{ExamRepository, QuestionsSet, TakeQuestionsSet, TakeQuestionsSetReply}

object ExamGenerator {

  type ExamOutput = (ExamRequest, Option[TeachersExam])

  def sampleExam(id: ExamId): TeachersExam = {
    val q1Answers = List(Answer("yes"), Answer("no"))
    val q1 = Question(BlankQuestion("Do you like scala?", q1Answers), List(Answer("yes")))
    val q2Answers = List(Answer("no"), Answer("yes"))
    val q2 = Question(BlankQuestion("Do you like akka?", q2Answers), List(Answer("no")))
    TeachersExam(id, List(q1, q2))
  }

  sealed trait ExamGenerator
  //incoming msg from ExamDistributor
  final case class ReceivedExamRequest(examRequest: ExamRequest) extends ExamGenerator

  final case class ReceivedSetFromRepo(set: TakeQuestionsSetReply) extends ExamGenerator

  case class State(requests: Set[ExamRequest])

  def apply(repository: ActorRef[ExamRepository])(distributor: ActorRef[ExamOutput])(state: State): Behavior[ExamGenerator] =
    generator(repository)(distributor)(state)

  sealed trait ExamGeneratorErrors
  private case class NotFoundResponse(examId: ExamId) extends ExamGeneratorErrors
  final case class EmptyRepositoryResponse(request: ExamRequest) extends ExamGeneratorErrors

  def generator(repository: ActorRef[ExamRepository])(distributor: ActorRef[ExamOutput])(state: State): Behavior[ExamGenerator] =
    Behaviors.setup { context =>
      val responseMapper: ActorRef[TakeQuestionsSetReply] =
        context.messageAdapter(response => ReceivedSetFromRepo(response))

      Behaviors.receiveMessage {
        case ReceivedExamRequest(examRequest@ExamRequest(examId, _, _, setId)) =>
          val newState = state.copy(requests = state.requests + examRequest)
          repository ! TakeQuestionsSet(setId, examId, responseMapper)
          generator(repository)(distributor)(newState)
        case ReceivedSetFromRepo(set@(examId, optionSet)) =>

          val eitherExam = for (
            i <- persistedRequest(state)(examId);
            j <- eitherQuestionsSet(i)(optionSet)
          ) yield (i, createExam(j)(i))

          eitherExam match {
            case Right(exam@(request, teachersExam)) =>
              distributor ! (request, Some(teachersExam))
              generator(repository)(distributor)(stateWithDropped(state)(examId))
            case Left(value) =>
              value match {
                case EmptyRepositoryResponse(request) =>
                  context.log.info("Repository returned no questions for examId:{} request", examId)
                  distributor ! (request, None)
                  generator(repository)(distributor)(stateWithDropped(state)(examId))
                case NotFoundResponse(examId) =>
                  context.log.error("Not found request corresponding to examId: {}! stopping the actor", examId)
                  Behaviors.stopped
              }
          }
      }
    }

  private def stateWithDropped(state: State)(idToDrop: ExamId) =
    state.copy(requests = state.requests.dropWhile(_.examId == idToDrop))

  private def persistedRequest(state: State)(examId: ExamId): Either[ExamGeneratorErrors, ExamRequest] =
    state.requests.find(_.examId == examId) match {
      case Some(value) => Right(value)
      case None => Left(NotFoundResponse(examId))
    }

  private def eitherQuestionsSet(request: ExamRequest)(questionsSet: Option[QuestionsSet]): Either[ExamGeneratorErrors, QuestionsSet] =
    questionsSet match {
      case Some(value) => Right(value)
      case None => Left(EmptyRepositoryResponse(request))
    }

  private[data] def createExam(questionsSet: QuestionsSet)(examRequest: ExamRequest) = {
    //todo: add taking random questions
    TeachersExam(examRequest.examId, questionsSet.questions.take(examRequest.maxQuestions).toList)
  }
}
