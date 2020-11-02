package exams.http

import japgolly.scalajs.react.ScalaComponent
import japgolly.scalajs.react.ScalazReact.ReactS
import org.scalajs.dom
import org.scalajs.dom.raw.Element
import org.scalajs.dom.{console, window}
import scalacss.DevDefaults.{cssEnv, cssStringRenderer}
import scalacss.ScalaCssReact._

object ScalaJs {

  val apiEndpoint: String = window.location.origin.getOrElse({
    console.log("unknown base url")
    ""
  })

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
          case ExamPage(token, exam) => ExamPageForm.renderExamForm(state, $, s)
          case ExamResultPage(score) => ExamResultPageForm.renderExamResultPageForm(state, $, s)
          case ErrorPage(reason) => ???
        }
      }
      ).build
  }
}
