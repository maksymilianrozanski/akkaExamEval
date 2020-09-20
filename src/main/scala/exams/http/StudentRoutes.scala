package exams.http

import akka.actor.typed.scaladsl.ActorContext
import akka.actor.typed.scaladsl.AskPattern.{Askable, schedulerFromActorSystem}
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpRequest}
import akka.http.scaladsl.server.Directives.{pathPrefix, _}
import akka.http.scaladsl.server.{Route, StandardRoute}
import akka.util.Timeout
import exams.data.TeachersExam.toStudentsExam
import exams.data._
import exams.http.StudentActions.ExamToDisplay
import exams.{ExamDistributor, RequestExamEvaluation}
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
    complete(userActions.ask(StudentActions.RequestExamCommand("hello", _, examDistributor)).mapTo[ExamToDisplay])
  }

  def examEvalRequested(request: HttpRequest)(implicit actors: RoutesActorsPack): Route = {
    //todo: replace generated exam with parsed JSON
    actors.examDistributor ! RequestExamEvaluation(CompletedExam(toStudentsExam(ExamGenerator.sampleExam())))
    println(s"exam eval endpoint, request: $request")
    complete(HttpEntity(ContentTypes.`text/plain(UTF-8)`, "exam eval endpoint, no content yet"))
  }
}

trait StudentsExamJsonProtocol extends DefaultJsonProtocol {
  implicit val answerFormat: RootJsonFormat[Answer] = jsonFormat1(Answer)
  implicit val blankQuestionFormat: RootJsonFormat[BlankQuestion] = jsonFormat3(BlankQuestion)
  implicit val studentsExamFormat: RootJsonFormat[StudentsExam] = jsonFormat1(StudentsExam)
  implicit val examToDisplayFormat: RootJsonFormat[ExamToDisplay] = jsonFormat1(ExamToDisplay)
}
