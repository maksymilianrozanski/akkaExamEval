package exams.data

//data
sealed trait Exam
final case class EmptyExam()extends Exam
final case class CompletedExam()extends  Exam
