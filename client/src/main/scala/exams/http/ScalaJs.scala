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

object ScalaJs {

  val apiEndpoint = "http://localhost:8080"

  def main(args: Array[String]): Unit = {
    val root = dom.document.getElementById("scalajsShoutOut")

    renderApp(root)(DisplayedState(Success, Some(ExamRequestPage(StudentsRequest("", 0, "")))))
  }

  def renderApp(root: Element)(page: DisplayedState) = {
    val state = ReactS.Fix[DisplayedState]
    requestExamForm(state, page)().renderIntoDOM(root)
  }

  def displayExamPage(state: ReactS.Fix[DisplayedState], examPage: DisplayedState) =
    ScalaComponent.builder[Unit]
      .initialState(examPage)
      .renderS(($, s) => {
        renderExam($, s)
      }).build

  private def renderExam($: Builder.Step3[Unit, DisplayedState, Unit]#$, s: DisplayedState) = {
    <.div(
      <.div(s"status: ${
        s.status.toString
      }"),
      <.div(s"Current exam: ${
        s.examPage.get.toString
      }"))
  }

  def requestExamForm(state: ReactS.Fix[DisplayedState], s: DisplayedState) = {
    val studentIdLens = GenLens[ExamRequestPage](_.studentsRequest.studentId)
    val maxQuestionsLens = GenLens[ExamRequestPage](_.studentsRequest.maxQuestions)
    val setIdLens = GenLens[ExamRequestPage](_.studentsRequest.setId)

    val pageOptional = Optional[DisplayedState, ExamRequestPage](_.examRequestPage)(n => m => m.copy(examRequestPage = Some(n)))

    val studentIdLens2 = pageOptional.composeLens(studentIdLens)
    val maxQuestionsLens2 = pageOptional.composeLens(maxQuestionsLens)
    val setIdLens2 = pageOptional.composeLens(setIdLens)

    def studentIdStateHandler(s: ReactEventFromInput) =
      state.mod(studentIdLens2.modify(_ => s.target.value))

    //todo: add int parse error handling
    def maxQuestionsStateHandler(s: ReactEventFromInput) =
      state.mod(maxQuestionsLens2.modify(_ => Integer.parseInt(s.target.value)))

    def setIdStateHandler(s: ReactEventFromInput) =
      state.mod(setIdLens2.modify(_ => s.target.value))

    def handleSubmit(e: ReactEventFromInput) = {
      (
        state.retM(e.preventDefaultCB) // Lift a Callback effect into a shape that allows composition
          //   with state modification.
          >> // Use >> to compose. It's flatMap (>>=) that ignores input.
          state.mod(s => {
            println("state: ", s)
            s
          }).liftCB // Here we lift a pure state modification into a shape that allows composition with Callback effects.
        )
    }

    def submitRequest(step3: Builder.Step3[Unit, DisplayedState, Unit]#$) = {
      val ajax = Ajax("POST", apiEndpoint + "/student/start2")
        .setRequestContentTypeJson
        .send(step3.state.examRequestPage.get.studentsRequest.asJson.noSpaces).onComplete {
        xhr =>
          xhr.status match {
            case 200 =>
              println("Sent request and received 200 response code")
              println(s"Response: ${xhr.responseText}")
              step3.setState(step3.state.copy(status = Success, examPage = decode[ExamPage](xhr.responseText).toOption))
            case x =>
              println(s"Sent request and received $x response code")
              step3.setState(step3.state.copy(status = Failure))
          }
      }
      step3.modState(i => i, ajax.asCallback)
    }

    def renderExamRequestForm($: Builder.Step3[Unit, DisplayedState, Unit]#$, s: DisplayedState) = {
      <.form(
        ^.onSubmit ==> {
          submitRequest($)
          $.runStateFn(handleSubmit)
        },
        <.p(s.status.toString),
        <.div(
          <.label(
            ^.`for` := "studentId",
            "Student Id"),
          <.input(
            ^.`type` := "text",
            ^.name := "studentId",
            ^.id := "studentId",
            ^.onChange ==> $.runStateFn(studentIdStateHandler)
          ),
          <.label(
            ^.`for` := "maxQuestions",
            "max. questions"
          ),
          <.input(
            ^.`type` := "number",
            ^.name := "maxQuestions",
            ^.id := "maxQuestions",
            ^.onChange ==> $.runStateFn(maxQuestionsStateHandler)
          ),
          <.label(
            ^.`for` := "setId",
            "set id"
          ),
          <.input(
            ^.`type` := "text",
            ^.name := "setId",
            ^.id := "setId",
            ^.onChange ==> $.runStateFn(setIdStateHandler)
          )),
        <.button("Submit", ^.onClick --> submitRequest($))
      )
    }

    ScalaComponent.builder[Unit]
      .initialState(s)
      .renderS(($, s) => {
        s match {
          case DisplayedState(status, Some(examRequestPage), None) =>
            renderExamRequestForm($, s)
          case DisplayedState(status, _, Some(examPage)) =>
            renderExam($, s)
          case _ => ???
        }
      }
      ).build
  }
}
