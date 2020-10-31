package exams.http

import exams.shared.data.HttpRequests.{SetId, StudentId, StudentsRequest}
import monocle.macros.GenLens
import monocle.{POptional, Prism}

object DisplayedState {

  val empty: ExamRequestPage = ExamRequestPage(StudentsRequest("", 0, ""))

  val examRequestPagePrism: Prism[DisplayedPage, ExamRequestPage] = Prism[DisplayedPage, ExamRequestPage] {
    case page: ExamRequestPage => Some(page)
    case _ => None
  }(identity)

  val examPagePrism: Prism[DisplayedPage, ExamPage] = Prism[DisplayedPage, ExamPage] {
    case page: ExamPage => Some(page)
    case _ => None
  }(identity)

  val examResultPagePrism: Prism[DisplayedPage, ExamResultPage] = Prism[DisplayedPage, ExamResultPage] {
    case page: ExamResultPage => Some(page)
    case _ => None
  }(identity)

  private val examRequestLens = GenLens[ExamRequestPage](_.studentsRequest)
  private val studentIdFromRequestLens = GenLens[StudentsRequest](_.studentId)
  private val maxQuestionsFromRequestLens = GenLens[StudentsRequest](_.maxQuestions)
  private val setIdFromRequestLens = GenLens[StudentsRequest](_.setId)
  private val examSelectableFromExamPageLens = GenLens[ExamPage](_.exam)
  private val questionSelectableLens = GenLens[ExamSelectable](_.questions)
  private val examSelectableLens = examPagePrism.composeLens(examSelectableFromExamPageLens)
  private val questionsSelectableLens = examSelectableLens.composeLens(questionSelectableLens)

  val studentIdLens: POptional[DisplayedPage, DisplayedPage, StudentId, StudentId] =
    examRequestPagePrism composeLens examRequestLens.composeLens(studentIdFromRequestLens)
  val maxQuestionsLens: POptional[DisplayedPage, DisplayedPage, Int, Int] =
    examRequestPagePrism composeLens examRequestLens.composeLens(maxQuestionsFromRequestLens)
  val setIdLens: POptional[DisplayedPage, DisplayedPage, SetId, SetId] =
    examRequestPagePrism composeLens examRequestLens.composeLens(setIdFromRequestLens)

  type QuestionIndex = Int
  type AnswerIndex = Int

  /**
   * @param valueToSet    isChecked value of AnswerSelectable to set
   * @param questionIndex index of a question where value should be updated
   * @param answerIndex   index of an answer where value should be updated
   * @return
   */
  def changeAnswerIsSelected(valueToSet: Boolean)(questionIndex: QuestionIndex, answerIndex: AnswerIndex): DisplayedPage => DisplayedPage =
    modifyAnswerOfQuestion(questionIndex, answerIndex)(_.copy(isChecked = valueToSet))

  private def modifyAnswerOfQuestion(qi: QuestionIndex, ai: AnswerIndex)(modifier: AnswerSelectable => AnswerSelectable) =
    modifyQuestion(qi, q => q.copy(answers = modifyAnswer(ai, modifier)(q.answers)))

  private def modifyQuestion(questionNumber: Int, modifier: BlankQuestionsSelectable => BlankQuestionsSelectable): DisplayedPage => DisplayedPage =
    questionsSelectableLens.modify(it => it.updated(questionNumber, modifier(it(questionNumber))))

  private def modifyAnswer(answerNumber: Int, modifier: AnswerSelectable => AnswerSelectable)(answers: List[AnswerSelectable]): List[AnswerSelectable] =
    answers.updated(answerNumber, modifier(answers(answerNumber)))

}
