package exams.http

import exams.http.ScalaJs.apiEndpoint
import japgolly.scalajs.react.ScalazReact.ReactS
import japgolly.scalajs.react.component.builder.Builder
import japgolly.scalajs.react.vdom.html_<^.<
import exams.shared.data.HttpRequests.{StudentId, StudentsRequest}
import exams.shared.data.{Answer, BlankQuestion, StudentsExam}
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

object ExamPageForm {
  def renderExamForm(state: ReactS.Fix[DisplayedState], $: Builder.Step3[Unit, DisplayedState, Unit]#$, s: DisplayedState) = {

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

    }

    <.div(
      <.div(s"status: ${
        s.status.toString
      }"),
      <.form(
        ^.onSubmit ==> {
          submitRequest($)
          $.runStateFn(handleSubmit)
        },
        <.div(s"Current exam: ${
          s.examPage.get.toString
        }")(s.examPage.get.exam.questions.map(blankQuestionForm): _*))
    )
  }

  private def blankQuestionForm(blankQuestion: BlankQuestionsSelectable) =
    <.div(
      <.p("question:"),
      <.p(blankQuestion.text)
      (blankQuestion.answers.zipWithIndex.map(answerForm): _*)
    )

  private def answerForm(answerWithKey: (AnswerSelectable, Int)) =
    <.label(s"answer: ${answerWithKey._1.text}",
      <.input.checkbox())
}
