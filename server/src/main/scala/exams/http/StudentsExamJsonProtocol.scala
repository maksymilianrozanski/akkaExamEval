package exams.http

import exams.data.ExamRepository.QuestionsSet
import exams.evaluator.ExamEvaluator
import exams.evaluator.ExamEvaluator.ExamResult
import exams.http.StudentActions.{DisplayedToStudent, ExamGeneratedWithToken, GeneratingFailed}
import exams.shared.data.HttpRequests.{ExamGenerated, StudentsRequest}
import exams.shared.data._
import exams.student.GiveResultToStudent
import spray.json.{DefaultJsonProtocol, JsValue, RootJsonFormat, RootJsonWriter}

trait StudentsExamJsonProtocol extends DefaultJsonProtocol {
  implicit val answerFormat: RootJsonFormat[Answer] = jsonFormat1(Answer)
  implicit val blankQuestionFormat: RootJsonFormat[BlankQuestion] = jsonFormat3(BlankQuestion)
  implicit val studentsExamFormat: RootJsonFormat[StudentsExam] = jsonFormat2(StudentsExam)
  implicit val completedExamFormat: RootJsonFormat[CompletedExam] = jsonFormat2(CompletedExam)
  implicit val examToDisplayFormat: RootJsonFormat[ExamGenerated] = jsonFormat1(ExamGenerated)
  implicit val generatingExamFailedFormat: RootJsonFormat[GeneratingFailed] = jsonFormat1(GeneratingFailed)
  implicit val studentsRequestFormat: RootJsonFormat[StudentsRequest] = jsonFormat3(StudentsRequest)
  implicit val questionFormat: RootJsonFormat[Question] = jsonFormat2(Question)
  implicit val questionsSetFormat: RootJsonFormat[QuestionsSet] = jsonFormat3(QuestionsSet)
  implicit val examResultFormat: RootJsonFormat[ExamResult] = jsonFormat3(ExamResult)
  implicit val examResultDisplayedToStudentFormat: RootJsonFormat[StudentActions.ExamResult] = jsonFormat1(StudentActions.ExamResult)

  implicit object DisplayedToStudentFormat extends RootJsonWriter[DisplayedToStudent] {

    override def write(obj: DisplayedToStudent): JsValue =
      obj match {
        case success: ExamGeneratedWithToken => examToDisplayFormat.write(ExamGenerated(success.exam))
        case failed: GeneratingFailed => generatingExamFailedFormat.write(failed)
        case result: StudentActions.ExamResult => examResultDisplayedToStudentFormat.write(result)
      }
  }
}
