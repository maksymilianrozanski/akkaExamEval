package exams.shared.data

import exams.shared.data.HttpRequests.ExamId

case class TeachersExam(examId: ExamId, questions: List[Question])
object TeachersExam {
  implicit def toStudentsExam(teachersExam: TeachersExam): StudentsExam =
    StudentsExam(teachersExam.examId, teachersExam.questions.map(_.blank))
}

case class StudentsExam(examId: ExamId, questions: List[BlankQuestion])

/**
 * @param blank          question displayed to the student (without selected answer)
 * @param correctAnswers list of correct answers (empty or more) - answers should be present in BlankQuestion
 */
case class Question(blank: BlankQuestion, correctAnswers: List[Answer])
/**
 * @param text    question text displayed to the student
 * @param answers list of possible answers
 */
case class BlankQuestion(text: String, answers: List[Answer], imageUrl: Option[String] = None)
/**
 * @param text text of answer
 */
case class Answer(text: String)
