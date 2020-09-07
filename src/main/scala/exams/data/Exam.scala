package exams.data

//data
sealed trait Exam
final case class EmptyExam(questions: List[BlankQuestion]) extends Exam
final case class CompletedExam(questions: List[BlankQuestion]) extends Exam

case class Question(question: BlankQuestion, correctAnswer: Answer)
case class BlankQuestion(text: String, answers: List[Answer], selectedAnswer: Option[Answer])
case class Answer(text: String)
