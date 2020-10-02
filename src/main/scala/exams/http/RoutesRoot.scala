package exams.http

import akka.actor.typed.scaladsl.AskPattern.{Askable, schedulerFromActorSystem}
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Directives.{pathPrefix, _}
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import exams.distributor.ExamDistributor.{ExamDistributor, RequestExamEvaluation}
import exams.data.ExamRepository.{AddQuestionsSet, ExamRepository, QuestionsSet}
import exams.data._
import exams.http.StudentActions.{DisplayedToStudent, ExamGenerated, GeneratingFailed, SendExamToEvaluation}
import spray.json._

import scala.concurrent.{ExecutionContext, Future}

case class RoutesActorsPack(userActions: ActorRef[StudentActions.Command],
                            system: ActorSystem[_],
                            examDistributor: ActorRef[ExamDistributor],
                            repository: ActorRef[ExamRepository],
                            implicit val timeout: Timeout)

object RoutesRoot extends StudentsExamJsonProtocol with SprayJsonSupport {

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

    RoutesRoot.allRoutes
  }

  def allRoutes(implicit studentsRequest: StudentsRequest => Future[DisplayedToStudent],
                completedExam: CompletedExam => Unit,
                addingQuestionsSet: QuestionsSet => Unit, ec: ExecutionContext): Route =
    StudentRoutes.studentRoutes ~ RepoRoutes.repoRoutes
}

trait StudentsExamJsonProtocol extends DefaultJsonProtocol {
  implicit val answerFormat: RootJsonFormat[Answer] = jsonFormat1(Answer)
  implicit val blankQuestionFormat: RootJsonFormat[BlankQuestion] = jsonFormat2(BlankQuestion)
  implicit val studentsExamFormat: RootJsonFormat[StudentsExam] = jsonFormat2(StudentsExam)
  implicit val completedExamFormat: RootJsonFormat[CompletedExam] = jsonFormat2(CompletedExam)
  implicit val examToDisplayFormat: RootJsonFormat[ExamGenerated] = jsonFormat1(ExamGenerated)
  implicit val generatingExamFailedFormat: RootJsonFormat[GeneratingFailed] = jsonFormat1(GeneratingFailed)
  implicit val studentsRequestFormat: RootJsonFormat[StudentsRequest] = jsonFormat3(StudentsRequest)
  implicit val questionFormat: RootJsonFormat[Question] = jsonFormat2(Question)
  implicit val questionsSetFormat: RootJsonFormat[QuestionsSet] = jsonFormat3(QuestionsSet)

  implicit object DisplayedToStudentFormat extends RootJsonWriter[DisplayedToStudent] {

    override def write(obj: DisplayedToStudent): JsValue =
      obj match {
        case success: ExamGenerated => examToDisplayFormat.write(success)
        case failed: GeneratingFailed => generatingExamFailedFormat.write(failed)
      }
  }
}
