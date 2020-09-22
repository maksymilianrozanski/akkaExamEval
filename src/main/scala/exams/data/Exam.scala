package exams.data

import exams.ExamDistributor.Answers
import exams.ExamDistributor.ExamId

//data
sealed trait Exam
final case class EmptyExam(questions: TeachersExam) extends Exam
final case class CompletedExam(examId: String, selectedAnswers: Answers) extends Exam

case class TeachersExam(examId: ExamId, questions: List[Question])
object TeachersExam {
  implicit def toStudentsExam(teachersExam: TeachersExam): StudentsExam =
    StudentsExam(teachersExam.examId, teachersExam.questions.map(_.question))
}

case class StudentsExam(examId: ExamId, questions: List[BlankQuestion])

case class Question(question: BlankQuestion, correctAnswers: List[String])
case class BlankQuestion(text: String, answers: List[Answer], selectedAnswers: List[String])
case class Answer(text: String)
