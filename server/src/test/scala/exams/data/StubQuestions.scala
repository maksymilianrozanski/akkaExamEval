package exams.data

import exams.shared.data
import exams.shared.data.{Answer, BlankQuestion, CompletedExam, Question}

object StubQuestions {
  val question1: Question = Question(BlankQuestion("question text", List(Answer("yes"), Answer("no"))), List(Answer("no")))
  val question2: Question = Question(BlankQuestion("question 2 text", List(Answer("yes"), Answer("no"))), List(Answer("yes")))
  val question3: Question = Question(BlankQuestion("question 3 text", List(Answer("yes"), Answer("no"))), List())
  val question4: Question = Question(BlankQuestion("new question", List(Answer("yes"), Answer("no"))), List(Answer("no")))
  val completedExam: CompletedExam = data.CompletedExam("exam123", List(List(Answer("1"), Answer("2")), List(Answer("yes"))))
}
