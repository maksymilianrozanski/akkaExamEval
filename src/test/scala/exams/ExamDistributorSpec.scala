package exams

import akka.actor.testkit.typed.scaladsl.TestInbox
import exams.ExamDistributor.{ExamAdded, ExamDistributorState, PersistedExam}
import exams.data.TeachersExam
import org.scalatest.wordspec.AnyWordSpecLike

class ExamDistributorSpec extends AnyWordSpecLike {

  "ExamDistributorState" must {
    //setup
    val student = TestInbox[Student]()
    val evaluator = TestInbox[ExamEvaluator]()
    val studentId = "123"
    //given
    val command = ExamDistributor.RequestExam(studentId, student.ref)
    def generator(id: String) = TeachersExam(id, List())

    "persist ExamAdded, empty initial state" in {
      val initialState = ExamDistributorState(Map())
      //when
      val result = ExamDistributor.onRequestExam(null)(generator)(initialState)(command)

      //then
      val persistedEvents = result.events
      val expectedEvents = Seq(ExamAdded("0", studentId, generator("0")))
      assert(expectedEvents == persistedEvents)
    }

    "persist ExamAdded, non-empty initial state" in {
      val initialState = ExamDistributorState(Map("123" -> PersistedExam("1234", generator("123"), None)))

      //when
      val result = ExamDistributor.onRequestExam(null)(generator)(initialState)(command)

      //then
      val persistedEvents = result.events
      val expectedEvents = Seq(ExamAdded("1", studentId, generator("1")))
      assert(expectedEvents == persistedEvents)
    }
  }

}
