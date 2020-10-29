package exams.http

import exams.http.AnswerSelectable.toAnswer
import exams.shared.data.HttpRequests._
import exams.shared.data.HttpResponses.ExamResult
import exams.shared.data.{Answer, BlankQuestion, StudentsExam}
import monocle.macros.GenLens
import monocle.{Lens, Optional, POptional}

sealed trait DisplayedPage
case class ExamRequestPage(studentsRequest: StudentsRequest) extends DisplayedPage
case class ExamPage(token: String, exam: ExamSelectable) extends DisplayedPage
case class ExamResultPage(score: Double) extends DisplayedPage

case class DisplayedState(status: RequestStatus,
                          examRequestPage: Option[ExamRequestPage] = None,
                          examPage: Option[ExamPage] = None,
                          examResultPage: Option[ExamResult] = None)

object DisplayedState {
  val empty: DisplayedState = DisplayedState(Success, Some(ExamRequestPage(StudentsRequest("", 0, ""))))

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

  type QuestionIndex = Int
  type AnswerIndex = Int

  /**
   * @param valueToSet    isChecked value of AnswerSelectable to set
   * @param questionIndex index of a question where value should be updated
   * @param answerIndex   index of an answer where value should be updated
   * @return
   */
  def changeAnswerIsSelected(valueToSet: Boolean)(questionIndex: QuestionIndex, answerIndex: AnswerIndex): DisplayedState => DisplayedState =
    modifyAnswerOfQuestion(questionIndex, answerIndex)(_.copy(isChecked = valueToSet))

  def withExamRemoved(displayedState: DisplayedState): DisplayedState =
    displayedState.copy(examPage = None)

  private def modifyAnswerOfQuestion(qi: QuestionIndex, ai: AnswerIndex)(modifier: AnswerSelectable => AnswerSelectable): DisplayedState => DisplayedState =
    modifyQuestion(qi, q => q.copy(answers = modifyAnswer(ai, modifier)(q.answers)))

  private def modifyQuestion(questionNumber: Int, modifier: BlankQuestionsSelectable => BlankQuestionsSelectable): DisplayedState => DisplayedState =
    questionsSelectableLens2.modify(it => it.updated(questionNumber, modifier(it(questionNumber))))

  private def modifyAnswer(answerNumber: Int, modifier: AnswerSelectable => AnswerSelectable)(answers: List[AnswerSelectable]): List[AnswerSelectable] =
    answers.updated(answerNumber, modifier(answers(answerNumber)))

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
