package exams.http

import exams.http.DisplayedState.{changeAnswerIsSelected, examPageOptional, questionsSelectableLens2}
import exams.http.ExamSelectable.toCompletedExam
import exams.http.ScalaJs.apiEndpoint
import japgolly.scalajs.react.ScalazReact.ReactS
import japgolly.scalajs.react.component.builder.Builder
import japgolly.scalajs.react.vdom.html_<^.<
import exams.shared.data.HttpRequests.{ExamGenerated, StudentId, StudentsRequest}
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
      //todo: implement sending answers to "/student/evaluate" api endpoint
      val ajax = Ajax("POST", apiEndpoint + "/student/evaluate")
        .setRequestContentTypeJson
        .setRequestHeader("Authorization", step3.state.examPage.get.token)
        .send(toCompletedExam(step3.state.examPage.get.exam).asJson.noSpaces)
        .onComplete {
          xhr =>
            xhr.status match {
              case 200 =>
                println("Sent request and received 200 response code")
                println(s"Response: ${xhr.responseText}")
                import ExamSelectable.fromStudentsExam
                step3.setState(step3.state)
              case x =>
                println(s"Sent request and received $x response code")
                step3.setState(step3.state)
            }
        }
      step3.modState(i => i, ajax.asCallback)
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
        }")(s.examPage.get.exam.questions
          .zipWithIndex
          .map(blankQuestionForm(state, $)): _*))
      , <.button("Submit", ^.onClick --> submitRequest($))
    )
  }

  private def blankQuestionForm(state: ReactS.Fix[DisplayedState], $: Builder.Step3[Unit, DisplayedState, Unit]#$)(
    blankQuestionWithNumber: (BlankQuestionsSelectable, Int)) =
    <.div(
      <.p("question:"),
      <.p(blankQuestionWithNumber._1.text)
      (blankQuestionWithNumber._1.answers.zipWithIndex.map(answerForm(state, $)(blankQuestionWithNumber._2)): _*)
    )

  private def answerForm(state: ReactS.Fix[DisplayedState], $: Builder.Step3[Unit, DisplayedState, Unit]#$)(questionNumber: Int)(answerWithKey: (AnswerSelectable, Int)) = {

    def onAnswerChange(e: ReactEventFromInput) =
      state.mod(changeAnswerIsSelected(e.target.checked)(questionNumber, answerWithKey._2))

    <.label(s"answer: ${answerWithKey._1.text}",
      <.input.checkbox(
        ^.onChange ==>
          $.runStateFn(onAnswerChange)
      ))
  }
}
