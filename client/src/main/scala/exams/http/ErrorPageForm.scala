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
import japgolly.scalajs.react.ScalazReact.ReactS
import japgolly.scalajs.react.component.builder.Builder

object ErrorPageForm {

  def renderErrorPage(state: ReactS.Fix[DisplayedPage], $: Builder.Step3[Unit, DisplayedPage, Unit]#$, s: DisplayedPage) = {
    val reason = DisplayedState.errorPagePrism.getOption(s).map(_.reason).getOrElse("unknown")
    <.p(s"Something went wrong ... \n probably because of: $reason")
  }
}
