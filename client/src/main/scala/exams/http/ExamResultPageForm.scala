package exams.http

import japgolly.scalajs.react.ScalazReact.ReactS
import japgolly.scalajs.react.component.builder.Builder
import japgolly.scalajs.react.vdom.html_<^.<
import exams.http.DisplayedState.{maxQuestionsLens2, setIdLens2, studentIdLens2}
import exams.http.ScalaJs.apiEndpoint
import exams.shared.data.HttpRequests.ExamGenerated
import io.circe.generic.auto._
import io.circe.parser.decode
import io.circe.syntax._
import japgolly.scalajs.react.ScalazReact.{ReactS, _}
import japgolly.scalajs.react.component.builder.Builder
import japgolly.scalajs.react.extra.Ajax
import japgolly.scalajs.react.vdom.html_<^.{<, ^, _}
import japgolly.scalajs.react.{ReactEventFromInput, _}

object ExamResultPageForm {

  def renderExamResultPageForm(state: ReactS.Fix[DisplayedState], $: Builder.Step3[Unit, DisplayedState, Unit]#$, s: DisplayedState) = {
    val score = s.examResultPage
      .map(i => {
        val scoreFormatted = f"${i.result}%1.2f"
        s"${i.studentId}, your score is $scoreFormatted out of 1.0"
      })
      .getOrElse("Something went wrong")
    <.p(score)
  }
}
