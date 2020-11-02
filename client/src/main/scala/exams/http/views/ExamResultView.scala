package exams.http.views

import exams.http.{DisplayedPage, ExamResultPage}
import exams.http.DisplayedState.examResultPagePrism
import exams.http.ScalaJs.Step3Builder
import japgolly.scalajs.react.ScalazReact.ReactS
import japgolly.scalajs.react.component.builder.Builder
import japgolly.scalajs.react.vdom.html_<^.{<, _}
import org.scalajs.dom.html.Paragraph

object ExamResultView {

  def apply(state: ReactS.Fix[DisplayedPage], $: Step3Builder, s: ExamResultPage): VdomTagOf[Paragraph] = {
    val resultText = examResultPagePrism.getOption(s)
      .map(i => {
        val scoreFormatted = f"${i.result.result}%1.2f"
        s"${i.result.studentId}, your score is $scoreFormatted out of 1"
      })
      .getOrElse("Something went wrong")
    <.p(resultText)
  }
}
