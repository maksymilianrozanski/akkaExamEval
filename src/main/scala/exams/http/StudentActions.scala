package exams.http

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import exams.distributor.ExamDistributor.{ExamDistributor, RequestExam, RequestExamEvaluation}
import exams.data.{StudentsExam, StudentsRequest}
import exams.student.Student

object StudentActions {

  sealed trait Command
  final case class RequestExamCommand2(studentsRequest: StudentsRequest, replyTo: ActorRef[DisplayedToStudent]) extends Command
  final case class SendExamToEvaluation(exam: RequestExamEvaluation) extends Command

  sealed trait DisplayedToStudent
  final case class ExamGenerated(exam: StudentsExam) extends DisplayedToStudent
  case class GeneratingFailed(reason: String) extends DisplayedToStudent

  def apply()(implicit distributor: ActorRef[ExamDistributor]): Behavior[Command] = registry(distributor)

  def registry(distributor: ActorRef[ExamDistributor]): Behavior[Command] = {
    Behaviors.setup(context =>
      Behaviors.receiveMessage {
        case RequestExamCommand2(studentsRequest, displayReceiver) =>
          context.log.info("received starting exam request(2)")
          val student = context.spawnAnonymous(Student(displayReceiver))
          distributor ! RequestExam(studentsRequest, student)
          Behaviors.same
        case SendExamToEvaluation(exam) =>
          distributor ! exam
          Behaviors.same
      })
  }
}
