package exams.http

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import exams.ExamDistributor.{ExamDistributor, RequestExam, RequestExam2, RequestExamEvaluation}
import exams.data.{StudentsExam, StudentsRequest}
import exams.student.Student

object StudentActions {

  sealed trait Command
  final case class RequestExamCommand(code: String, replyTo: ActorRef[ExamToDisplay]) extends Command
  final case class RequestExamCommand2(studentsRequest: StudentsRequest, replyTo: ActorRef[ExamToDisplay]) extends Command
  final case class SendExamToEvaluation(exam: RequestExamEvaluation) extends Command

  final case class ExamToDisplay(exam: StudentsExam)

  def apply()(implicit distributor: ActorRef[ExamDistributor]): Behavior[Command] = registry(distributor)

  def registry(distributor: ActorRef[ExamDistributor]): Behavior[Command] = {
    Behaviors.setup(context =>
      Behaviors.receiveMessage {
        case RequestExamCommand(code, displayReceiver) =>
          context.log.info("received starting exam request")
          val student = context.spawnAnonymous(Student(displayReceiver))
          distributor ! RequestExam(code, student)
          Behaviors.same
        case RequestExamCommand2(studentsRequest, displayReceiver) =>
          context.log.info("received starting exam request(2)")
          val student = context.spawnAnonymous(Student(displayReceiver))
          distributor ! RequestExam2(studentsRequest, student)
          Behaviors.same
        case SendExamToEvaluation(exam) =>
          distributor ! exam
          Behaviors.same
      })
  }
}
