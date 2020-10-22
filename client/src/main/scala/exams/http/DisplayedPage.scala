package exams.http

import exams.shared.data.HttpRequests.{ExamId, SetId, StudentId, StudentsRequest}
import exams.shared.data.{Answer, BlankQuestion, StudentsExam}
import monocle.{Lens, Optional, POptional}
import monocle.macros.GenLens
import monocle.Traversal
import cats.implicits._

sealed trait DisplayedPage
case class ExamRequestPage(studentsRequest: StudentsRequest) extends DisplayedPage
case class ExamPage(exam: ExamSelectable) extends DisplayedPage

case class DisplayedState(status: RequestStatus, examRequestPage: Option[ExamRequestPage] = None, examPage: Option[ExamPage] = None)

object DisplayedState {
  private val studentIdLens: Lens[ExamRequestPage, StudentId] =
    GenLens[ExamRequestPage](_.studentsRequest.studentId)
  private val maxQuestionsLens: Lens[ExamRequestPage, Int] =
    GenLens[ExamRequestPage](_.studentsRequest.maxQuestions)
  private val setIdLens: Lens[ExamRequestPage, SetId] =
    GenLens[ExamRequestPage](_.studentsRequest.setId)

  val pageOptional: Optional[DisplayedState, ExamRequestPage] =
    Optional[DisplayedState, ExamRequestPage](_.examRequestPage)(n => m => m.copy(examRequestPage = Some(n)))

  val examPageOptional: Optional[DisplayedState, ExamPage] =
    Optional[DisplayedState, ExamPage](_.examPage)(n => m => m.copy(examPage = Some(n)))

  val examSelectableLens: Lens[ExamPage, ExamSelectable] =
    GenLens[ExamPage](_.exam)

  val examSelectableLens2: POptional[DisplayedState, DisplayedState, ExamSelectable, ExamSelectable] =
    examPageOptional.composeLens(examSelectableLens)

  private val questionsSelectableLens = GenLens[ExamSelectable](_.questions)
  val questionsSelectableLens2: POptional[DisplayedState, DisplayedState, List[BlankQuestionsSelectable], List[BlankQuestionsSelectable]] =
    examSelectableLens2.composeLens(questionsSelectableLens)

  //todo: rename
  def answerOfQuestionsLens2(questionNumber: Int, answerNumber: Int)(modifier: AnswerSelectable => AnswerSelectable): DisplayedState => DisplayedState =
    questionsSelectableLens2.modify(
      (it: List[BlankQuestionsSelectable]) =>
        it.updated(questionNumber,
          it(questionNumber).copy(answers = it(questionNumber).answers.updated(answerNumber, modifier(it(questionNumber).answers(answerNumber))
          ))))

  //todo: rename
  def answersOfQuestionModifier(questionNumber: Int, modifier: BlankQuestionsSelectable => BlankQuestionsSelectable): DisplayedState => DisplayedState =
    questionsSelectableLens2.modify(it => it.updated(questionNumber, modifier(it(questionNumber))))

  //todo: rename
  def answerModifier(answerNumber: Int, modifier: AnswerSelectable => AnswerSelectable)(answers: List[AnswerSelectable]): List[AnswerSelectable] =
    answers.updated(answerNumber, modifier(answers(answerNumber)))

  //todo: rename
  def isSelectedLens(valueToSet: Boolean): (Int, Int) => DisplayedState => DisplayedState =
    answerOfQuestionsLens2(_: Int, _: Int)(it => it.copy(isChecked = valueToSet))

  val studentIdLens2: POptional[DisplayedState, DisplayedState, StudentId, StudentId] =
    pageOptional.composeLens(studentIdLens)
  val maxQuestionsLens2: POptional[DisplayedState, DisplayedState, Int, Int] =
    pageOptional.composeLens(maxQuestionsLens)
  val setIdLens2: POptional[DisplayedState, DisplayedState, SetId, SetId] =
    pageOptional.composeLens(setIdLens)
}

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
}
case class BlankQuestionsSelectable(text: String, answers: List[AnswerSelectable], number: Int)
object BlankQuestionsSelectable {
  implicit def fromBlankQuestion(questionNumber: (BlankQuestion, Int)): BlankQuestionsSelectable =
    BlankQuestionsSelectable(questionNumber._1.text,
      questionNumber._1.answers.zipWithIndex.map(AnswerSelectable.fromAnswer), questionNumber._2)
}
