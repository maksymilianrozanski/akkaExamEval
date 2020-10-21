package exams.http

import exams.shared.data.HttpRequests.{SetId, StudentId, StudentsRequest}
import exams.shared.data.StudentsExam
import monocle.{Lens, Optional, POptional}
import monocle.macros.GenLens

sealed trait DisplayedPage
case class ExamRequestPage(studentsRequest: StudentsRequest) extends DisplayedPage
case class ExamPage(exam: StudentsExam) extends DisplayedPage

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
