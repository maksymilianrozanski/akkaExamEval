package exams.http

import japgolly.scalajs.react.ScalazReact.ReactS
import japgolly.scalajs.react.component.builder.Builder
import japgolly.scalajs.react.vdom.html_<^.<
import exams.http.DisplayedState.examResultPagePrism
import exams.http.ScalaJs.apiEndpoint
import io.circe.generic.auto._
import io.circe.parser.decode
import io.circe.syntax._
import japgolly.scalajs.react.ScalazReact.{ReactS, _}
import japgolly.scalajs.react.component.builder.Builder
import japgolly.scalajs.react.extra.Ajax
import japgolly.scalajs.react.vdom.html_<^.{<, ^, _}
import japgolly.scalajs.react.{ReactEventFromInput, _}

object ExamResultPageForm {

  def renderExamResultPageForm(state: ReactS.Fix[DisplayedPage], $: Builder.Step3[Unit, DisplayedPage, Unit]#$, s: DisplayedPage) = {
    val resultText = examResultPagePrism.getOption(s)
      .map(i => {
        val scoreFormatted = f"${i.result.result}%1.2f"
        s"${i.result.studentId}, your score is $scoreFormatted out of 1"
      })
      .getOrElse("Something went wrong")
    <.p(resultText)
  }
}
