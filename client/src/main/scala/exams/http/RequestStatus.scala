package exams.http

sealed trait RequestStatus
case object Success extends RequestStatus
case object Failure extends RequestStatus
