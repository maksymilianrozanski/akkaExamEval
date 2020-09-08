package exams.data

object ExamGenerator {

  def sampleExam(): TeachersExam = {
    val q1Answers = List(Answer("yes"), Answer("no"))
    val q1 = Question(
      BlankQuestion("Do you like scala?", q1Answers, List.empty), List(0))
    val q2Answers = List(Answer("no"), Answer("yes"))
    val q2 = Question(
      BlankQuestion("Do you like akka?", q2Answers, List.empty), List(1))
    TeachersExam(List(q1, q2))
  }

}
