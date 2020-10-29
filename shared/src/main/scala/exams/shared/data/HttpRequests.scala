package exams.shared.data

object HttpRequests {

  type ExamId = String
  type StudentId = String
  type SetId = String
  type Answers = List[List[Answer]]

  /**
   * Request of generating an exam
   *
   * @param studentId    student identifier
   * @param maxQuestions maximal number of questions in generated exam
   * @param setId        id of questions set from which questions are requested
   */
  case class StudentsRequest(studentId: StudentId, maxQuestions: Int, setId: SetId)

  /**
   * Exam completed by a student
   *
   * @param examId          generated unique exam identifier
   * @param answers answers selected by a student, list indexes corresponding to question number
   */
  case class CompletedExam(examId: String, answers: List[List[Answer]])
}
