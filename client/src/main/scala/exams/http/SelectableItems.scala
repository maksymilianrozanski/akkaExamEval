package exams.http

import exams.http.AnswerSelectable.toAnswer
import exams.shared.data.HttpRequests._
import exams.shared.data.HttpResponses.ExamResult
import exams.shared.data.{Answer, BlankQuestion, StudentsExam}
import monocle.macros.GenLens
import monocle.{Lens, Optional, POptional}

case class AnswerSelectable(answer: Answer, isChecked: Boolean, number: Int)
object AnswerSelectable {
  implicit def toAnswer(answerSelectable: AnswerSelectable): Answer =
    answerSelectable.answer

  implicit def fromAnswer(answerNumber: (Answer, Int)): AnswerSelectable =
    AnswerSelectable(answerNumber._1, isChecked = false, answerNumber._2)
}

case class ExamSelectable(examId: ExamId, questions: List[BlankQuestionsSelectable])
object ExamSelectable {
  implicit def fromStudentsExam(studentsExam: StudentsExam): ExamSelectable =
    ExamSelectable(studentsExam.examId, studentsExam.questions.zipWithIndex
      .map(BlankQuestionsSelectable.fromBlankQuestion))

  implicit def toCompletedExam(exam: ExamSelectable): CompletedExam =
    CompletedExam(exam.examId, exam.questions.map(_.answers.filter(_.isChecked).map(toAnswer)))
}
case class BlankQuestionsSelectable(text: String, answers: List[AnswerSelectable], number: Int, imageUrl: Option[String] = None)
object BlankQuestionsSelectable {
  implicit def fromBlankQuestion(questionNumber: (BlankQuestion, Int)): BlankQuestionsSelectable = {
    BlankQuestionsSelectable(questionNumber._1.text,
      questionNumber._1.answers.zipWithIndex.map(AnswerSelectable.fromAnswer),
      questionNumber._2,
      questionNumber._1.imageUrl)
  }
}
