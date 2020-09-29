package exams.http

import akka.actor.typed.scaladsl.ActorContext
import akka.actor.typed.scaladsl.AskPattern.{Askable, schedulerFromActorSystem}
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpRequest}
import akka.http.scaladsl.server.Directives.{pathPrefix, _}
import akka.http.scaladsl.server.{Route, StandardRoute}
import akka.util.Timeout
import exams.ExamDistributor.{ExamDistributor, RequestExamEvaluation}
import exams.data.ExamRepository.{AddQuestionsSet, ExamRepository, QuestionsSet}
import exams.data._
import exams.http.StudentActions.{ExamToDisplay, SendExamToEvaluation}
import spray.json._

import scala.concurrent.Future

case class RoutesActorsPack(userActions: ActorRef[StudentActions.Command],
                            system: ActorSystem[_],
                            examDistributor: ActorRef[ExamDistributor],
                            repository: ActorRef[ExamRepository],
                            implicit val timeout: Timeout)

object StudentRoutes2 extends StudentsExamJsonProtocol with SprayJsonSupport {

  def createStudentRoutes(implicit actors: RoutesActorsPack): Route = {
    implicit val actorSystem: ActorSystem[_] = actors.system

    import actors._

    implicit def examRequestedFuture: StudentsRequest => Future[ExamToDisplay] =
      (request: StudentsRequest) => actors.userActions.ask((replyTo: ActorRef[ExamToDisplay]) =>
        StudentActions.RequestExamCommand2(request, replyTo))

    StudentRoutes2.studentRoutes
  }

  def studentRoutes(implicit actors: RoutesActorsPack,
                    actorSystem: ActorSystem[_],
                    studentsRequest: StudentsRequest => Future[ExamToDisplay]): Route = {
    pathPrefix("student") {
      (pathEndOrSingleSlash & get) {
        complete(
          HttpEntity(ContentTypes.`text/plain(UTF-8)`, "nothing here yet"))
      } ~ post {
        path("start2") {
          examRequestedRoute3
        }
      } ~ (path("evaluate") & post & extractRequest) {
        examEvalRequested
      }
    } ~ pathPrefix("repo") {
      (path("add") & post & extractRequest) {
        addSetToRepo
      }
    }
  }

  def examRequestedRoute3(implicit future: StudentsRequest => Future[ExamToDisplay]): Route =
    entity(as[StudentsRequest])(request => complete(future(request)))

  def examEvalRequested(request: HttpRequest)(implicit actors: RoutesActorsPack): Route = {
    println(s"exam eval endpoint, request: $request")
    entity(as[CompletedExam]) {
      exam =>
        actors.userActions ! SendExamToEvaluation(RequestExamEvaluation(exam.examId, exam.selectedAnswers))
        complete(HttpEntity(ContentTypes.`text/plain(UTF-8)`, "requested exam evaluation"))
    }
  }

  def addSetToRepo(request: HttpRequest)(implicit actors: RoutesActorsPack): Route = {
    entity(as[QuestionsSet]) {
      set =>
        actors.repository ! AddQuestionsSet(set)
        complete(HttpEntity(ContentTypes.`text/plain(UTF-8)`, "requested adding questions set"))
    }
  }
}

trait StudentsExamJsonProtocol extends DefaultJsonProtocol {
  implicit val answerFormat: RootJsonFormat[Answer] = jsonFormat1(Answer)
  implicit val blankQuestionFormat: RootJsonFormat[BlankQuestion] = jsonFormat2(BlankQuestion)
  implicit val studentsExamFormat: RootJsonFormat[StudentsExam] = jsonFormat2(StudentsExam)
  implicit val completedExamFormat: RootJsonFormat[CompletedExam] = jsonFormat2(CompletedExam)
  implicit val examToDisplayFormat: RootJsonFormat[ExamToDisplay] = jsonFormat1(ExamToDisplay)
  implicit val studentsRequestFormat: RootJsonFormat[StudentsRequest] = jsonFormat3(StudentsRequest)
  implicit val questionFormat: RootJsonFormat[Question] = jsonFormat2(Question)
  implicit val questionsSetFormat: RootJsonFormat[QuestionsSet] = jsonFormat3(QuestionsSet)
}
