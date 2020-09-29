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

    implicit def examCompletedFuture: CompletedExam => Unit =
      (exam: CompletedExam) =>
        actors.userActions ! SendExamToEvaluation(RequestExamEvaluation(exam.examId, exam.selectedAnswers))

    implicit def addingQuestionsSet: QuestionsSet => Unit =
      (set: QuestionsSet) => actors.repository ! AddQuestionsSet(set)

    StudentRoutes2.studentRoutes
  }

  def studentRoutes(implicit studentsRequest: StudentsRequest => Future[ExamToDisplay],
                    completedExam: CompletedExam => Unit,
                    addingQuestionsSet: QuestionsSet => Unit): Route = {
    pathPrefix("student") {
      (pathEndOrSingleSlash & get) {
        complete(
          HttpEntity(ContentTypes.`text/plain(UTF-8)`, "nothing here yet"))
      } ~ post {
        path("start2") {
          examRequestedRoute
        }
      } ~ (path("evaluate") & post & extractRequest) { _ =>
        examEvalRequested
      }
    } ~ pathPrefix("repo") {
      (path("add") & post & extractRequest) { _ =>
        addSetToRepo
      }
    }
  }

  def examRequestedRoute(implicit future: StudentsRequest => Future[ExamToDisplay]): Route =
    entity(as[StudentsRequest])(request => complete(future(request)))

  def examEvalRequested(implicit future: CompletedExam => Unit): Route = {
    entity(as[CompletedExam]) {
      exam =>
        println(s"exam eval endpoint, request: $exam")
        future(exam)
        complete(HttpEntity(ContentTypes.`text/plain(UTF-8)`, "requested exam evaluation"))
    }
  }

  def addSetToRepo(implicit addingQuestions: QuestionsSet => Unit): Route =
    entity(as[QuestionsSet]) { set =>
      addingQuestions(set)
      complete(HttpEntity(ContentTypes.`text/plain(UTF-8)`, "requested adding questions set"))
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
