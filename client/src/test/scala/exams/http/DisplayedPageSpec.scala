package exams.http

import exams.http.DisplayedState.changeAnswerIsSelected
import exams.shared.data.Answer
import exams.shared.data.HttpRequests.StudentsRequest
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

class DisplayedPageSpec extends AnyWordSpecLike with Matchers {

  private val testExamPage: ExamPage = ExamPage("", ExamSelectable("exam20", List(
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
  )))

  "changeAnswerIsSelected" should {
    val displayedState: DisplayedPage = testExamPage

    "copy DisplayedPage with changed value of: 'question index 1, answer index 0' to checked" in {
      val expected = ExamPage("", ExamSelectable("exam20", List(
        testExamPage.exam.questions(0),
        testExamPage.exam.questions(1).copy(answers = List(
          testExamPage.exam.questions(1).answers(0).copy(isChecked = true),
          testExamPage.exam.questions(1).answers(1)
        )),
        testExamPage.exam.questions(2)
      )))

      val result = changeAnswerIsSelected(true)(1, 0)(displayedState)
      assertResult(expected)(result)
    }

    """copy DisplayedPage with changed value of: 'question index 0, answer index 1' to checked,
      | and 'question index 2, answer index 2' to checked  """.stripMargin in {

      val expected = ExamPage("", ExamSelectable("exam20", List(
        testExamPage.exam.questions(0).copy(answers = List(
          testExamPage.exam.questions(0).answers(0),
          testExamPage.exam.questions(0).answers(1).copy(isChecked = true)
        )),
        testExamPage.exam.questions(1),
        testExamPage.exam.questions(2).copy(answers = List(
          testExamPage.exam.questions(2).answers(0),
          testExamPage.exam.questions(2).answers(1),
          testExamPage.exam.questions(2).answers(2).copy(isChecked = true),
        )))))

      val result1 = changeAnswerIsSelected(true)(0, 1)(displayedState)
      val result2 = changeAnswerIsSelected(true)(2, 2)(result1)
      assertResult(expected)(result2)
    }

    "copy DisplayedPage with changed value of: 'question index 2, answer index 1' to unchecked" in {
      val expected = ExamPage("", ExamSelectable("exam20", List(
        testExamPage.exam.questions(0).copy(answers = List(
          testExamPage.exam.questions(0).answers(0),
          testExamPage.exam.questions(0).answers(1)
        )),
        testExamPage.exam.questions(1),
        testExamPage.exam.questions(2).copy(answers = List(
          testExamPage.exam.questions(2).answers(0),
          testExamPage.exam.questions(2).answers(1).copy(isChecked = false),
          testExamPage.exam.questions(2).answers(2),
        )))))

      val result = changeAnswerIsSelected(false)(2, 1)(displayedState)
      assertResult(expected)(result)
    }

    "return unchanged state if trying to check already checked value" in {
      val result = changeAnswerIsSelected(false)(1, 0)(displayedState)
      assertResult(displayedState)(result)
    }

    "return unchanged state if trying to uncheck already unchecked value" in {
      val result = changeAnswerIsSelected(true)(2, 1)(displayedState)
      assertResult(displayedState)(result)
    }
  }
}
