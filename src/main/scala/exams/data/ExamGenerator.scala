package exams.data

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import exams.ExamDistributor.{ExamDistributor, ExamId}
import exams.data.ExamRepository.{ExamRepository, QuestionsSet, SetId, TakeQuestionsSet, TakeQuestionsSetReply}

object ExamGenerator {

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

  def apply(repository: ActorRef[ExamRepository])(distributor: ActorRef[ExamDistributor])(state: State): Behavior[ExamGenerator] =
    generator(repository)(distributor)(state)

  def generator(repository: ActorRef[ExamRepository])(distributor: ActorRef[ExamDistributor])(state: State): Behavior[ExamGenerator] =
    Behaviors.setup { context =>
      val responseMapper: ActorRef[TakeQuestionsSetReply] =
        context.messageAdapter(response => ReceivedSetFromRepo(response))

      Behaviors.receiveMessage {
        case ReceivedExamRequest(examRequest@ExamRequest(examId, _, _, setId)) =>
          val newState = state.copy(requests = state.requests + examRequest)
          repository ! TakeQuestionsSet(setId, examId, responseMapper)
          generator(repository)(distributor)(newState)
        case ReceivedSetFromRepo(set) =>
          //get ExamRequest from current state

          //create TeachersExam

          //send TeachersExam to ExamDistributor

          //remove ExamRequest from state
          ???
      }
    }

  private[data] def createExam(questionsSet: QuestionsSet)(examRequest: ExamRequest) = {
    //todo: add taking random questions
    TeachersExam(examRequest.examId, questionsSet.questions.take(examRequest.maxQuestions).toList)
  }
}
