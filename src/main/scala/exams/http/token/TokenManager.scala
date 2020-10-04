package exams.http.token

import exams.data.StudentsExam

object TokenManager {

  implicit def tokenFromExam(exam: StudentsExam): String =
    TokenGenerator.createToken(exam.examId, 7)(System.currentTimeMillis, TokenGenerator.secretKey)

}
