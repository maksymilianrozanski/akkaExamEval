package exams.http

import exams.shared.data.HttpRequests.{StudentId, StudentsRequest}
import exams.shared.data.StudentsExam
import japgolly.scalajs.react.raw.ReactDOMServer
import japgolly.scalajs.react.{Callback, CtorType, React, ReactEventFromInput, ScalaComponent, ScalaFnComponent, StateAccess, StateAccessPure, _}
import org.scalajs.dom
import org.scalajs.dom.html.Div
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs._
import japgolly.scalajs.react.ScalazReact.{ReactS, reactCallbackScalazInstance}
import japgolly.scalajs.react.extra.{Ajax, StateSnapshot}
import scalaz.Scalaz.ToBindOps
import japgolly.scalajs.react.ScalazReact._
import japgolly.scalajs.react.component.Scala.Component
import japgolly.scalajs.react.component.builder.Builder
import japgolly.scalajs.react.vdom.html_<^._
import scalaz.StateT.stateMonad
import scalaz.effect.MonadIO.stateTMonadIO
import io.circe._
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._
import org.scalajs.dom.raw.Element
import monocle.{Lens, Optional, POptional, Prism}
import monocle.macros.GenLens
import scalacss.DevDefaults.{cssEnv, cssStringRenderer}
import scalacss.ScalaCssReact._

object ScalaJs {

  val apiEndpoint = "http://localhost:8080"

  def main(args: Array[String]): Unit = {
    QuestionStyles.addToDocument()
    val root = dom.document.getElementById("scalajsShoutOut")
    renderApp(root)(DisplayedState.empty)
  }

  def renderApp(root: Element)(page: DisplayedPage) = {
    val state = ReactS.Fix[DisplayedPage]
    rootComponent(state, page)().renderIntoDOM(root)
  }

  def rootComponent(state: ReactS.Fix[DisplayedPage], s: DisplayedPage) = {
    ScalaComponent.builder[Unit]
      .initialState(s)
      .renderS(($, s) => {
        s match {
          case ExamRequestPage(studentsRequest) => ExamRequestPageForm.renderExamRequestForm(state, $, s)
          case ExamPage(token, exam) =>ExamPageForm.renderExamForm(state, $, s)
          case ExamResultPage(score) =>ExamResultPageForm.renderExamResultPageForm(state, $, s)
          case ErrorPage(reason) => ???
        }
      }
      ).build
  }
}
