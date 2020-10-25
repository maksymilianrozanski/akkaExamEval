package exams.http

import exams.http.DisplayedState.{changeAnswerIsSelected, withExamRemoved}
import exams.http.ExamSelectable.toCompletedExam
import exams.http.ScalaJs.apiEndpoint
import io.circe.generic.auto._
import io.circe.syntax._
import japgolly.scalajs.react.AsyncCallback.unit.>>=
import japgolly.scalajs.react.ScalazReact.{ReactS, reactCallbackScalazInstance, _}
import japgolly.scalajs.react.component.builder.Builder
import japgolly.scalajs.react.extra.Ajax
import japgolly.scalajs.react.vdom.html_<^.{<, _}
import japgolly.scalajs.react.{ReactEventFromInput, _}
import scalaz.Scalaz.{ToBindOps, function1Covariant}

object ExamPageForm {
  def renderExamForm(state: ReactS.Fix[DisplayedState], $: Builder.Step3[Unit, DisplayedState, Unit]#$, s: DisplayedState) = {

    def submitRequest(step3: Builder.Step3[Unit, DisplayedState, Unit]#$) = {
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
                step3.setState(withExamRemoved(step3.state))
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
        ^.onSubmit ==> {(_: ReactEventFromInput).preventDefaultCB},
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
      <.p(blankQuestionWithNumber._1.text,
        blankQuestionWithNumber._1.imageUrl.whenDefined(url => <.img(^.src := url)))
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
