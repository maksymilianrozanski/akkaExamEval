package exams.http

import exams.shared.data.HttpRequests.StudentsRequest
import exams.shared.data.StudentsExam

sealed trait DisplayedPage
case class ExamRequestPage(studentsRequest: StudentsRequest) extends DisplayedPage
case class ExamPage(exam: StudentsExam) extends DisplayedPage

case class DisplayedState(status: RequestStatus, examRequestPage: Option[ExamRequestPage] = None, examPage: Option[ExamPage] = None)
