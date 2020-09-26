package exams.data

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import exams.ExamDistributor.ExamId
import exams.data.ExamRepository.{ExamRepository, QuestionsSet, SetId, TakeQuestionsSet}

object ExamGenerator {

  def sampleExam(id: ExamId): TeachersExam = {
    val q1Answers = List(Answer("yes"), Answer("no"))
    val q1 = Question(BlankQuestion("Do you like scala?", q1Answers), List(Answer("yes")))
    val q2Answers = List(Answer("no"), Answer("yes"))
    val q2 = Question(BlankQuestion("Do you like akka?", q2Answers), List(Answer("no")))
    TeachersExam(id, List(q1, q2))
  }

}
