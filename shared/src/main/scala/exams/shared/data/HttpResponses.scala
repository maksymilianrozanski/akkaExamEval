package exams.shared.data

object HttpResponses {

  /**
   * Item of an array returned at GET /repo/results endpoint
   *
   * @param examId    generated unique exam identifier
   * @param studentId not unique name provided by user
   * @param result    score of an exam (0.0 - 1.0)
   */
  case class ExamResult(examId: String, studentId: String, result: Double)

  /**
   * Generated exam returned at POST /student/start2 endpoint
   *
   * @param exam exam given to a student (without correct answers)
   */
  case class ExamGenerated(exam: StudentsExam)

}
