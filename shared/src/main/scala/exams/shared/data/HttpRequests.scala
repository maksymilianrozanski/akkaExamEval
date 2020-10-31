package exams.shared.data

object HttpRequests {

  type ExamId = String
  type StudentId = String
  type SetId = String
  type Answers = List[List[Answer]]

  /**
   * Request of generating an exam
   * payload of POST /student/start2 api endpoint
   *
   * @param studentId    student identifier
   * @param maxQuestions maximal number of questions in generated exam
   * @param setId        id of questions set from which questions are requested
   */
  case class StudentsRequest(studentId: StudentId, maxQuestions: Int, setId: SetId)

  /**
   * Exam completed by a student
   * payload of POST /student/evaluate api endpoint
   *
   * @param examId  generated unique exam identifier
   * @param answers answers selected by a student, list indexes corresponding to question number
   */
  case class CompletedExam(examId: String, answers: List[List[Answer]])

  /**
   * @param setId unique id of questions set
   * @param description questions set description
   * @param questions set of blank questions with correct answers
   */
  case class QuestionsSet(setId: SetId, description: String, questions: Set[Question])
}
