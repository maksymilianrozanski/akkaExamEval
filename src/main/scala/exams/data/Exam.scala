package exams.data

import exams.ExamDistributor.{Answers, ExamId, StudentId}
import exams.data.ExamRepository.SetId

//data
sealed trait Exam
final case class EmptyExam(questions: TeachersExam) extends Exam
final case class CompletedExam(examId: String, selectedAnswers: Answers) extends Exam

case class TeachersExam(examId: ExamId, questions: List[Question])
object TeachersExam {
  implicit def toStudentsExam(teachersExam: TeachersExam): StudentsExam =
    StudentsExam(teachersExam.examId, teachersExam.questions.map(_.blank))
}

case class StudentsExam(examId: ExamId, questions: List[BlankQuestion])

case class Question(blank: BlankQuestion, correctAnswers: List[Answer])
case class BlankQuestion(text: String, answers: List[Answer])
case class Answer(text: String)

case class ExamRequest(examId: ExamId, studentId: StudentId, maxQuestions: Int, setId: SetId)
