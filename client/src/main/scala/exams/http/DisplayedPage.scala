package exams.http

import exams.shared.data.HttpRequests.StudentsRequest
import exams.shared.data.HttpResponses.ExamResult

sealed trait DisplayedPage
case class ExamRequestPage(studentsRequest: StudentsRequest) extends DisplayedPage
case class ExamPage(token: String, exam: ExamSelectable) extends DisplayedPage
case class ExamResultPage(result: ExamResult) extends DisplayedPage
case class ErrorPage(reason: String) extends DisplayedPage


