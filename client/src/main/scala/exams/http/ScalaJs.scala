package exams.http

import exams.shared.data.HttpRequests.{StudentId, StudentsRequest}
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
import monocle.Lens
import monocle.macros.GenLens

object ScalaJs {

  val apiEndpoint = "http://localhost:8080"

  def main(args: Array[String]): Unit = {
    val root = dom.document.getElementById("scalajsShoutOut")

    renderApp(root)(ExamRequestPage(Success, StudentsRequest("", 0, "")))
  }

  def renderApp(root: Element)(page: DisplayedPage) =
    (page match {
      case examRequestPage@ExamRequestPage(status, studentsRequest) => requestExamForm(examRequestPage)()
      case ExamPage(status) => ???
    }).renderIntoDOM(root)

  def requestExamForm(page: ExamRequestPage) = {
    val state = ReactS.Fix[ExamRequestPage]

    val studentIdLens = GenLens[ExamRequestPage](_.studentsRequest.studentId)
    val maxQuestionsLens = GenLens[ExamRequestPage](_.studentsRequest.maxQuestions)
    val setIdLens = GenLens[ExamRequestPage](_.studentsRequest.setId)

    def studentIdStateHandler(s: ReactEventFromInput) =
      state.mod(studentIdLens.modify(_ => s.target.value))

    //todo: add int parse error handling
    def maxQuestionsStateHandler(s: ReactEventFromInput) =
      state.mod(maxQuestionsLens.modify(_ => Integer.parseInt(s.target.value)))

    def setIdStateHandler(s: ReactEventFromInput) =
      state.mod(setIdLens.modify(_ => s.target.value))

    def handleSubmit(e: ReactEventFromInput) = {
      (
        state.retM(e.preventDefaultCB) // Lift a Callback effect into a shape that allows composition
          //   with state modification.
          >> // Use >> to compose. It's flatMap (>>=) that ignores input.
          state.mod(s => {
            println("state: ", s)
            s
          }).liftCB // Here we lift a pure state modification into a shape that
        )
    }

    def submitRequest(step3: Builder.Step3[Unit, ExamRequestPage, Unit]#$) = {
      val ajax = Ajax("POST", apiEndpoint + "/student/start2")
        .setRequestContentTypeJson
        .send(step3.state.studentsRequest.asJson.noSpaces).onComplete {
        xhr =>
          xhr.status match {
            case 200 =>
              println("Sent request and received 200 response code")
              println(s"Response: ${xhr.responseText}")
              step3.setState(step3.state.copy(status = Success))
            case x =>
              println(s"Sent request and received $x response code")
              step3.setState(step3.state.copy(status = Failure))
          }
      }
      step3.modState(i => i, ajax.asCallback)
    }

    import japgolly.scalajs.react.extra.Ajax

    ScalaComponent.builder[Unit]
      .initialState(page)
      .renderS(($, s) => {
        <.form(
          ^.onSubmit ==> {
            submitRequest($)
            $.runStateFn(handleSubmit)
          },
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
              ^.value := s.studentsRequest.setId,
              ^.onChange ==> $.runStateFn(setIdStateHandler)
            )),
          <.button("Submit", ^.onClick --> submitRequest($))
        )
      }
      ).build
  }
}
