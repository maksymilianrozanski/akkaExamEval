package exams.data

import exams.distributor.ExamDistributor.{Answers, ExamId, StudentId}
import exams.data.ExamRepository.SetId

//data
sealed trait Exam
final case class EmptyExam(questions: TeachersExam) extends Exam

/**
 * Exam completed by a student
 *
 * @param examId          generated unique exam identifier
 * @param selectedAnswers answers selected by a student, list indexes corresponding to question number
 */
final case class CompletedExam(examId: String, selectedAnswers: Answers) extends Exam

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
case class BlankQuestion(text: String, answers: List[Answer])
/**
 * @param text text of answer
 */
case class Answer(text: String)
/**
 * Request of generating an exam
 *
 * @param studentId    student identifier
 * @param maxQuestions maximal number of questions in generated exam
 * @param setId        id of questions set from which questions are requested
 */
case class StudentsRequest(studentId: StudentId, maxQuestions: Int, setId: SetId)
case class ExamRequest(examId: ExamId, studentId: StudentId, maxQuestions: Int, setId: SetId)
