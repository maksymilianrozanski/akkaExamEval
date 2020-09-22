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
    StudentsExam(teachersExam.examId, teachersExam.questions.map(_.blank))

  def withSelectedAnswers(exam: TeachersExam)(answers: Answers): TeachersExam =
    exam.copy(questions = exam.questions.zip(answers).map {
      case (question, answers) => Question.withSelectedAnswers(question)(answers)
    })
}

case class StudentsExam(examId: ExamId, questions: List[BlankQuestion])

case class Question(blank: BlankQuestion, correctAnswers: List[String])
case class BlankQuestion(text: String, answers: List[Answer], selectedAnswers: List[Answer])
case class Answer(text: String)

object Question {

  def withSelectedAnswers(question: Question)(answers: List[Answer]): Question = {
    Question(correctAnswers = question.correctAnswers,
      blank = question.blank.copy(selectedAnswers = answers)
    )
  }
}
