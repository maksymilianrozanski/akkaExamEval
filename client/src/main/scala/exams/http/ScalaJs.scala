package exams.http

import exams.shared.data.HttpRequests.{StudentId, StudentsRequest}
import japgolly.scalajs.react.raw.ReactDOMServer
import japgolly.scalajs.react.{Callback, CtorType, React, ReactEventFromInput, ScalaComponent, ScalaFnComponent, StateAccess, StateAccessPure, _}
import org.scalajs.dom
import org.scalajs.dom.html.Div
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs._
import japgolly.scalajs.react.ScalazReact.{ReactS, reactCallbackScalazInstance}
import japgolly.scalajs.react.extra.StateSnapshot
import scalaz.Scalaz.ToBindOps
import japgolly.scalajs.react.ScalazReact._
import japgolly.scalajs.react.component.Scala.Component
import japgolly.scalajs.react.vdom.html_<^._
import scalaz.StateT.stateMonad
import scalaz.effect.MonadIO.stateTMonadIO

object ScalaJs {

  val apiEndpoint = "localhost:8080"

  def main(args: Array[String]): Unit = {
    val root = dom.document.getElementById("scalajsShoutOut")

    val examForm = requestExamForm()()
    examForm.renderIntoDOM(root)
  }

  def requestExamForm() = {
    val ST = ReactS.Fix[StudentsRequest]

    def studentIdStateHandler(s: ReactEventFromInput) =
      ST.mod(_.copy(studentId = s.target.value))

    def maxQuestionsStateHandler(s: ReactEventFromInput) =
      ST.mod(_.copy(maxQuestions = Integer.parseInt(s.target.value)))

    def setIdStateHandler(s: ReactEventFromInput) =
      ST.mod(_.copy(setId = s.target.value))

    def handleSubmit(e: ReactEventFromInput) = {
      (
        ST.retM(e.preventDefaultCB) // Lift a Callback effect into a shape that allows composition
          //   with state modification.
          >> // Use >> to compose. It's flatMap (>>=) that ignores input.
          ST.mod(s => {
            println("state: ", s)
            s
          }).liftCB // Here we lift a pure state modification into a shape that
        )
    }

    ScalaComponent.builder[Unit]
      .initialState(StudentsRequest("", 0, ""))
      .renderS(($, s) =>
        <.form(
          ^.onSubmit ==> $.runStateFn(handleSubmit),
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
              ^.value := s.setId,
              ^.onChange ==> $.runStateFn(setIdStateHandler)
            )),
          <.button("Submit")
        )
      ).build
  }
}
