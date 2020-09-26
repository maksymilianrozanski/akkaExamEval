package exams.data

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.persistence.testkit.scaladsl.EventSourcedBehaviorTestKit
import org.scalatest.wordspec.AnyWordSpecLike

class ExamGeneratorSpec extends ScalaTestWithActorTestKit(EventSourcedBehaviorTestKit.config) with AnyWordSpecLike {

  "ExamGenerator" when {

    "receive ExamRequest" should {

      "send message to ExamRepository" in {

      }

      "add ExamRequest to state" in {

      }
    }

    "receive questions set" should {

      "send generated TeachersExam to ExamDistributor" in {

      }

      "remove ExamRequest from state" in {

      }
    }
  }

  "createExam" when {

    "maxQuestions is higher or equal size of set" should {

      "create exam containing all set's questions" in {

      }
    }

    "maxQuestions is lower than size of set" should {

      "create exam containing maxQuestions number of questions" in {

      }
    }
  }
}
