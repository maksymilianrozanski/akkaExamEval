package exams.data

object StubQuestions {
  val question1: Question = Question(BlankQuestion("question text", List(Answer("yes"), Answer("no"))), List(Answer("no")))
  val question2: Question = Question(BlankQuestion("question 2 text", List(Answer("yes"), Answer("no"))), List(Answer("yes")))
  val question3: Question = Question(BlankQuestion("question 3 text", List(Answer("yes"), Answer("no"))), List())
  val question4: Question = Question(BlankQuestion("new question", List(Answer("yes"), Answer("no"))), List(Answer("no")))
}
