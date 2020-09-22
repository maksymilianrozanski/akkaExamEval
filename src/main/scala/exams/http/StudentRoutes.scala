package exams.http

import akka.actor.typed.scaladsl.ActorContext
import akka.actor.typed.scaladsl.AskPattern.{Askable, schedulerFromActorSystem}
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpRequest}
import akka.http.scaladsl.server.Directives.{pathPrefix, _}
import akka.http.scaladsl.server.{Route, StandardRoute}
import akka.util.Timeout
import exams.ExamDistributor.{ExamDistributor, RequestExamEvaluationCompact}
import exams.data._
import exams.http.StudentActions.{ExamToDisplay, SendExamToEvaluation}
import spray.json._

case class RoutesActorsPack(userActions: ActorRef[StudentActions.Command],
                            context: ActorContext[_],
                            system: ActorSystem[_],
                            examDistributor: ActorRef[ExamDistributor],
                            implicit val timeout: Timeout)

object StudentRoutes2 extends StudentsExamJsonProtocol with SprayJsonSupport {

  def createStudentRoutes(implicit actors: RoutesActorsPack): Route = {
    implicit val actorSystem: ActorSystem[_] = actors.system
    StudentRoutes2.studentRoutes
  }

  def studentRoutes(implicit actors: RoutesActorsPack, actorSystem: ActorSystem[_]): Route = {
    pathPrefix("student") {
      (pathEndOrSingleSlash & get) {
        complete(
          HttpEntity(ContentTypes.`text/plain(UTF-8)`, "nothing here yet"))
      } ~ (path("start") & get) {
        examRequestedRoute
      } ~ (path("evaluate") & post & extractRequest) {
        examEvalRequested
      }
    }
  }

  def examRequestedRoute(implicit actors: RoutesActorsPack, actorSystem: ActorSystem[_]): StandardRoute = {
    import actors._
    complete(userActions.ask((replyTo: ActorRef[ExamToDisplay]) => StudentActions.RequestExamCommand("hello", replyTo)).mapTo[ExamToDisplay])
  }

  def examEvalRequested(request: HttpRequest)(implicit actors: RoutesActorsPack): Route = {
    println(s"exam eval endpoint, request: $request")
    entity(as[CompletedExam]) {
      exam =>
        actors.userActions ! SendExamToEvaluation(RequestExamEvaluationCompact(exam.examId, exam.selectedAnswers))
        complete(HttpEntity(ContentTypes.`text/plain(UTF-8)`, "requested exam evaluation"))
    }
  }
}

trait StudentsExamJsonProtocol extends DefaultJsonProtocol {
  implicit val answerFormat: RootJsonFormat[Answer] = jsonFormat1(Answer)
  implicit val blankQuestionFormat: RootJsonFormat[BlankQuestion] = jsonFormat2(BlankQuestion)
  implicit val studentsExamFormat: RootJsonFormat[StudentsExam] = jsonFormat2(StudentsExam)
  implicit val completedExamFormat: RootJsonFormat[CompletedExam] = jsonFormat2(CompletedExam)
  implicit val examToDisplayFormat: RootJsonFormat[ExamToDisplay] = jsonFormat1(ExamToDisplay)
}
