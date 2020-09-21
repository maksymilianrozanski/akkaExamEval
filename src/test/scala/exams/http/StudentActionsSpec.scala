package exams.http

import akka.actor.testkit.typed.scaladsl.{BehaviorTestKit, TestInbox}
import exams.ExamDistributor.{ExamDistributor, RequestExamEvaluationCompact}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should

class StudentActionsSpec extends AnyFlatSpec with should.Matchers {

  "StudentActions" should "redirect message to ExamDistributor" in {
    val inbox = TestInbox[ExamDistributor]()
    val testKit = BehaviorTestKit(StudentActions()(inbox.ref))
    val messageContent = RequestExamEvaluationCompact("exam123", List(List("1"), List("2", "3")))
    testKit.run(StudentActions.SendExamToEvaluation(messageContent))
    inbox.expectMessage(messageContent)
  }
}
