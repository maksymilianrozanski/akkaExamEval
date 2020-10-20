package exams.http

import exams.shared.data.HttpRequests.StudentsRequest

sealed trait DisplayedPage
case class ExamRequestPage(status: RequestStatus, studentsRequest: StudentsRequest) extends DisplayedPage
case class ExamPage(status: RequestStatus) extends DisplayedPage
