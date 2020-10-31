package exams.http

import exams.shared.data.HttpRequests.{StudentId, StudentsRequest}
import monocle.macros.GenLens
import monocle.{Optional, POptional, Prism}

object DisplayedState {

  val empty: ExamRequestPage = ExamRequestPage(StudentsRequest("", 0, ""))

  val examRequestPagePrism2 = Prism[DisplayedPage, ExamRequestPage] {
    case page@ExamRequestPage(studentsRequest) => Some(page)
    case _ => None
  } {
    i => i
  }

  val examPagePrism2 = Prism[DisplayedPage, ExamPage] {
    case page@ExamPage(token, exam) => Some(page)
    case _ => None
  } {
    i => i
  }

  val examResultPagePrism = Prism[DisplayedPage, ExamResultPage] {
    case page@ExamResultPage(result) => Some(page)
    case _ => None
  } {
    i => i
  }

  private val examRequestPagePrism =
    Prism[DisplayedPage, StudentsRequest] {
      case ExamRequestPage(studentsRequest) =>
        Some(studentsRequest)
      case _ => None
    }(ExamRequestPage)

  private val examPagePrism =
    Prism[DisplayedPage, (String, ExamSelectable)] {
      case ExamPage(token, exam) =>
        Some((token, exam))
      case _ => None
    } {
      case (token, exam) => ExamPage(token, exam)
    }

  private val studentIdLens = GenLens[StudentsRequest](_.studentId)
  private val maxQuestionsLens = GenLens[StudentsRequest](_.maxQuestions)
  private val setIdLens = GenLens[StudentsRequest](_.setId)
  private val examSelectableLens = GenLens[(String, ExamSelectable)](_._2)
  private val questionSelectableLens = GenLens[ExamSelectable](_.questions)
  private val examSelectableLens2 = examPagePrism.composeLens(examSelectableLens)
  private val questionsSelectableLens2 = examSelectableLens2.composeLens(questionSelectableLens)

  val studentIdLens2: POptional[DisplayedPage, DisplayedPage, StudentId, StudentId] =
    examRequestPagePrism.composeLens(studentIdLens)
  val maxQuestionsLens2 = examRequestPagePrism.composeLens(maxQuestionsLens)
  val setIdLens2 = examRequestPagePrism.composeLens(setIdLens)

  type QuestionIndex = Int
  type AnswerIndex = Int

  /**
   * @param valueToSet    isChecked value of AnswerSelectable to set
   * @param questionIndex index of a question where value should be updated
   * @param answerIndex   index of an answer where value should be updated
   * @return
   */
  def changeAnswerIsSelected(valueToSet: Boolean)(questionIndex: QuestionIndex, answerIndex: AnswerIndex) =
    modifyAnswerOfQuestion(questionIndex, answerIndex)(_.copy(isChecked = valueToSet))

  private def modifyAnswerOfQuestion(qi: QuestionIndex, ai: AnswerIndex)(modifier: AnswerSelectable => AnswerSelectable) =
    modifyQuestion(qi, q => q.copy(answers = modifyAnswer(ai, modifier)(q.answers)))

  private def modifyQuestion(questionNumber: Int, modifier: BlankQuestionsSelectable => BlankQuestionsSelectable): DisplayedPage => DisplayedPage =
    questionsSelectableLens2.modify(it => it.updated(questionNumber, modifier(it(questionNumber))))

  private def modifyAnswer(answerNumber: Int, modifier: AnswerSelectable => AnswerSelectable)(answers: List[AnswerSelectable]): List[AnswerSelectable] =
    answers.updated(answerNumber, modifier(answers(answerNumber)))

}
