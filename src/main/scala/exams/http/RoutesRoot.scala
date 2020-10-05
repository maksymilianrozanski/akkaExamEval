package exams.http

import akka.actor.typed.scaladsl.AskPattern.{Askable, schedulerFromActorSystem}
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import exams.data.ExamRepository.{AddQuestionsSet, ExamRepository, QuestionsSet}
import exams.data._
import exams.distributor.ExamDistributor.{ExamDistributor, ExamId, RequestExamEvaluation}
import exams.evaluator.ExamEvaluator
import exams.evaluator.ExamEvaluator.RequestResults
import exams.http.StudentActions.{DisplayedToStudent, SendExamToEvaluation}
import exams.http.token.TokenGenerator
import exams.http.token.TokenGenerator.{TokenValidationResult, ValidMatchedToken}

import scala.concurrent.{ExecutionContext, Future}

case class RoutesActorsPack(userActions: ActorRef[StudentActions.Command],
                            system: ActorSystem[_],
                            examDistributor: ActorRef[ExamDistributor],
                            repository: ActorRef[ExamRepository],
                            evaluator: ActorRef[ExamEvaluator.ExamEvaluator],
                            implicit val timeout: Timeout)

object RoutesRoot extends StudentsExamJsonProtocol with SprayJsonSupport {

  type ExamTokenValidator = (String, ExamId) => Either[TokenValidationResult, ValidMatchedToken]
  type AllExamResults = () => Future[List[ExamEvaluator.ExamResult]]

  def createStudentRoutes(implicit actors: RoutesActorsPack): Route = {
    implicit val actorSystem: ActorSystem[_] = actors.system
    import actorSystem.executionContext
    import actors._

    implicit def examRequestedFuture: StudentsRequest => Future[DisplayedToStudent] =
      (request: StudentsRequest) => actors.userActions.ask((replyTo: ActorRef[DisplayedToStudent]) =>
        StudentActions.RequestExamCommand2(request, replyTo))

    implicit def examCompletedFuture: CompletedExam => Unit =
      (exam: CompletedExam) =>
        actors.userActions ! SendExamToEvaluation(RequestExamEvaluation(exam.examId, exam.selectedAnswers))

    implicit def addingQuestionsSet: QuestionsSet => Unit =
      (set: QuestionsSet) => actors.repository ! AddQuestionsSet(set)

    implicit def examTokenValidator: ExamTokenValidator =
      TokenGenerator.validateToken(_, _)(System.currentTimeMillis, TokenGenerator.secretKey)

    implicit def requestAllResults: AllExamResults = () =>
      actors.evaluator.ask(RequestResults)

    RoutesRoot.allRoutes
  }

  def allRoutes(implicit studentsRequest: StudentsRequest => Future[DisplayedToStudent],
                completedExam: CompletedExam => Unit,
                addingQuestionsSet: QuestionsSet => Unit, ec: ExecutionContext,
                examTokenValidator: ExamTokenValidator,
                examResults: AllExamResults): Route =
    StudentRoutes.studentRoutes ~ RepoRoutes.repoRoutes
}
