package exams.http

import exams.http.views.{ErrorView, ExamRequestView, ExamResultView, ExamView}
import japgolly.scalajs.react.{CtorType, ScalaComponent}
import japgolly.scalajs.react.ScalazReact.ReactS
import japgolly.scalajs.react.component.Scala.{Component, Unmounted}
import japgolly.scalajs.react.component.builder.Builder
import org.scalajs.dom
import org.scalajs.dom.raw.Element
import org.scalajs.dom.{console, window}
import scalacss.DevDefaults.{cssEnv, cssStringRenderer}
import scalacss.ScalaCssReact._

object ScalaJs {

  type Step3Builder = Builder.Step3[Unit, DisplayedPage, Unit]#$

  val apiEndpoint: String = window.location.origin.getOrElse({
    console.log("unknown base url")
    ""
  })

  def main(args: Array[String]): Unit = {
    QuestionStyles.addToDocument()
    val root = dom.document.getElementById("scalajsShoutOut")
    renderApp(root)(DisplayedState.empty)
  }

  def renderApp(root: Element)(page: DisplayedPage): Unmounted[Unit, DisplayedPage, Unit]#Mounted = {
    val state = ReactS.Fix[DisplayedPage]
    rootComponent(state, page)().renderIntoDOM(root)
  }

  def rootComponent(state: ReactS.Fix[DisplayedPage], s: DisplayedPage): Component[Unit, DisplayedPage, Unit, CtorType.Nullary] = {
    ScalaComponent.builder[Unit]
      .initialState(s)
      .renderS(($, s) => {
        s match {
          case _: ExamRequestPage => ExamRequestView(state, $, s)
          case _: ExamPage => ExamView(state, $, s)
          case _: ExamResultPage => ExamResultView(state, $, s)
          case _: ErrorPage => ErrorView(state, $, s)
        }
      }
      ).build
  }
}
