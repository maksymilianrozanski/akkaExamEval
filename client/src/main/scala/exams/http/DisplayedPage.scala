package exams.http

import exams.shared.data.HttpRequests.{ExamId, SetId, StudentId, StudentsRequest}
import exams.shared.data.{Answer, BlankQuestion, StudentsExam}
import monocle.{Lens, Optional, POptional}
import monocle.macros.GenLens

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

  val studentIdLens2: POptional[DisplayedState, DisplayedState, StudentId, StudentId] =
    pageOptional.composeLens(studentIdLens)
  val maxQuestionsLens2: POptional[DisplayedState, DisplayedState, Int, Int] =
    pageOptional.composeLens(maxQuestionsLens)
  val setIdLens2: POptional[DisplayedState, DisplayedState, SetId, SetId] =
    pageOptional.composeLens(setIdLens)
}

case class AnswerSelectable(answer: Answer, isChecked: Boolean)
object AnswerSelectable {
  implicit def toAnswer(answerSelectable: AnswerSelectable): Answer =
    answerSelectable.answer

  implicit def fromAnswer(answer: Answer): AnswerSelectable =
    AnswerSelectable(answer, isChecked = false)
}

case class ExamSelectable(examId: ExamId, questions: List[BlankQuestionsSelectable])
object ExamSelectable {
  implicit def fromStudentsExam(studentsExam: StudentsExam): ExamSelectable =
    ExamSelectable(studentsExam.examId, studentsExam.questions.map(BlankQuestionsSelectable.fromBlankQuestion))
}
case class BlankQuestionsSelectable(text: String, answers: List[AnswerSelectable])
object BlankQuestionsSelectable {
  implicit def fromBlankQuestion(blankQuestion: BlankQuestion): BlankQuestionsSelectable =
    BlankQuestionsSelectable(blankQuestion.text, blankQuestion.answers.map(AnswerSelectable.fromAnswer))
}
