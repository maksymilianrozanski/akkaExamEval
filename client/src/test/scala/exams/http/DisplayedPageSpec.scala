package exams.http

import exams.http.DisplayedState.changeAnswerIsSelected
import exams.shared.data
import exams.shared.data.HttpRequests.StudentsRequest
import exams.shared.data.{Answer, BlankQuestion, CompletedExam, Question, StudentsExam}
import org.scalatest._
import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.matchers.should.Matchers
import org.scalatest.verbs.ShouldVerb

//noinspection ZeroIndexToHead
class DisplayedPageSpec extends AnyWordSpecLike with Matchers {
  private val testExamPage: Option[ExamPage] = Option(ExamPage(ExamSelectable("exam20", List(
    BlankQuestionsSelectable("question text", List(
      AnswerSelectable(Answer("yes"), isChecked = false, 0),
      AnswerSelectable(Answer("no"), isChecked = false, 1)), 0),
    BlankQuestionsSelectable("question 2 text", List(
      AnswerSelectable(Answer("yes"), isChecked = false, 0),
      AnswerSelectable(Answer("no"), isChecked = false, 1)), 1),
    BlankQuestionsSelectable("question 3 text", List(
      AnswerSelectable(Answer("yes"), isChecked = false, 0),
      AnswerSelectable(Answer("no"), isChecked = true, 1),
      AnswerSelectable(Answer("unknown"), isChecked = false, 2)), 2)
  ))))

  "changeAnswerIsSelected" should {
    val examRequestPage = Option(ExamRequestPage(StudentsRequest("student1", 3, "set1")))
    val displayedState = DisplayedState(Success, examRequestPage, testExamPage)

    "copy DisplayedState with changed value of: 'question index 1, answer index 0' to checked" in {
      val expected = DisplayedState(Success, examRequestPage, Option(ExamPage(
        ExamSelectable("exam20", List(
          testExamPage.get.exam.questions(0),
          testExamPage.get.exam.questions(1).copy(answers = List(
            testExamPage.get.exam.questions(1).answers(0).copy(isChecked = true),
            testExamPage.get.exam.questions(1).answers(1)
          )),
          testExamPage.get.exam.questions(2)
        )))))

      val result = changeAnswerIsSelected(true)(1, 0)(displayedState)
      assertResult(expected)(result)
    }

    """copy DisplayedState with changed value of: 'question index 0, answer index 1' to checked,
      | and 'question index 2, answer index 2' to checked  """.stripMargin in {

      val expected = DisplayedState(Success, examRequestPage, Option(ExamPage(
        ExamSelectable("exam20", List(
          testExamPage.get.exam.questions(0).copy(answers = List(
            testExamPage.get.exam.questions(0).answers(0),
            testExamPage.get.exam.questions(0).answers(1).copy(isChecked = true)
          )),
          testExamPage.get.exam.questions(1),
          testExamPage.get.exam.questions(2).copy(answers = List(
            testExamPage.get.exam.questions(2).answers(0),
            testExamPage.get.exam.questions(2).answers(1),
            testExamPage.get.exam.questions(2).answers(2).copy(isChecked = true),
          )))))))

      val result1 = changeAnswerIsSelected(true)(0, 1)(displayedState)
      val result2 = changeAnswerIsSelected(true)(2, 2)(result1)
      assertResult(expected)(result2)
    }

    "copy DisplayedState with changed value of: 'question index 2, answer index 1' to unchecked" in {
      val expected = DisplayedState(Success, examRequestPage, Option(ExamPage(
        ExamSelectable("exam20", List(
          testExamPage.get.exam.questions(0).copy(answers = List(
            testExamPage.get.exam.questions(0).answers(0),
            testExamPage.get.exam.questions(0).answers(1)
          )),
          testExamPage.get.exam.questions(1),
          testExamPage.get.exam.questions(2).copy(answers = List(
            testExamPage.get.exam.questions(2).answers(0),
            testExamPage.get.exam.questions(2).answers(1).copy(isChecked = false),
            testExamPage.get.exam.questions(2).answers(2),
          )))))))

      val result = changeAnswerIsSelected(false)(2, 1)(displayedState)
      assertResult(expected)(result)
    }

    "return unchanged state if trying to check already checked value" in {
      val result = changeAnswerIsSelected(false)(1,0)(displayedState)
      assertResult(displayedState)(result)
    }

    "return unchanged state if trying to uncheck already unchecked value" in {
      val result = changeAnswerIsSelected(true)(2, 1)(displayedState)
      assertResult(displayedState)(result)
    }
  }
}
