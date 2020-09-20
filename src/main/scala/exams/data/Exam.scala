package exams.data

import exams.ExamDistributor.Answers

//data
sealed trait Exam
final case class EmptyExam(questions: TeachersExam) extends Exam
final case class CompletedExam(examId: String, selectedAnswers: Answers) extends Exam

case class TeachersExam(questions: List[Question])
object TeachersExam {
  implicit def toStudentsExam(teachersExam: TeachersExam): StudentsExam =
    StudentsExam(teachersExam.questions.map(_.question))
}

case class StudentsExam(questions: List[BlankQuestion])

case class Question(question: BlankQuestion, correctAnswer: List[Int])
case class BlankQuestion(text: String, answers: List[Answer], selectedAnswer: List[Int])
case class Answer(text: String)
