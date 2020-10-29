package exams.shared.data

object HttpResponses {

  case class ExamResult(examId: String, studentId: String, result: Double)

  case class ExamGenerated(exam: StudentsExam)

}
