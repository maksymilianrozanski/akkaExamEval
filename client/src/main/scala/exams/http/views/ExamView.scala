package exams.http.views

import exams.http.DisplayedState.{changeAnswerIsSelected, examPagePrism}
import exams.http.ExamSelectable.toCompletedExam
import exams.http.ScalaJs.{Step3Builder, apiEndpoint}
import exams.http._
import exams.shared.data.HttpResponses.ExamResult
import io.circe.generic.auto._
import io.circe.parser.decode
import io.circe.syntax._
import japgolly.scalajs.react.ScalazReact.{ReactS, _}
import japgolly.scalajs.react.component.builder.Builder
import japgolly.scalajs.react.extra.Ajax
import japgolly.scalajs.react.vdom.html_<^.{<, ^, _}
import japgolly.scalajs.react.{ReactEventFromInput, _}
import org.scalajs.dom.html.Div
import scalacss.ScalaCssReact._

object ExamView {
  def apply(state: ReactS.Fix[DisplayedPage], $: Step3Builder, s: ExamPage): VdomTagOf[Div] = {

    def submitRequest(step3: Step3Builder) = {
      val ajax = Ajax("POST", apiEndpoint + "/student/evaluate")
        .setRequestContentTypeJson
        .setRequestHeader("Authorization",
          examPagePrism.getOption(step3.state).map(_.token).getOrElse(""))
        .send(toCompletedExam(examPagePrism.getOption(step3.state).get.exam).asJson.noSpaces)
        .onComplete {
          xhr =>
            xhr.status match {
              case 200 =>
                println("Sent request and received 200 response code")
                println(s"Response: ${xhr.responseText}")

                decode[ExamResult](xhr.responseText).toOption match {
                  case Some(examResult) => step3.setState(ExamResultPage(examResult))
                  case None => step3.setState(ErrorPage("Invalid server response"))
                }

              case x =>
                println(s"Sent request and received $x response code")
                step3.setState(ErrorPage(s"Sent request and received $x response code"))
            }
        }
      step3.modState(i => i, ajax.asCallback)
    }

    <.div(
      <.form(
        ^.onSubmit ==> {(_: ReactEventFromInput).preventDefaultCB},
        <.div(examPagePrism.getOption(s).get.exam.questions
          .zipWithIndex
          .map(blankQuestionForm(state, $)): _*))
      , <.button("Submit", ^.onClick --> submitRequest($))
    )
  }

  private def blankQuestionForm(state: ReactS.Fix[DisplayedPage], $: Step3Builder)(
    blankQuestionWithNumber: (BlankQuestionsSelectable, Int)) =
    <.div(QuestionStyles.questionContainer,
      <.div(blankQuestionWithNumber._1.text,
        <.div(
          blankQuestionWithNumber._1.imageUrl.whenDefined(url => <.img(QuestionStyles.imageStyle, ^.src := url)))
      ),
      <.ul(blankQuestionWithNumber._1.answers.zipWithIndex.map(answerForm(state, $)(blankQuestionWithNumber._2)): _*)
    )

  private def answerForm(state: ReactS.Fix[DisplayedPage], $: Step3Builder)(questionNumber: Int)(answerWithKey: (AnswerSelectable, Int)) = {

    def onAnswerChange(e: ReactEventFromInput) =
      state.mod(changeAnswerIsSelected(e.target.checked)(questionNumber, answerWithKey._2))

    <.li(
      <.label(s"${answerWithKey._1.text}",
        <.input.checkbox(
          ^.onChange ==>
            $.runStateFn(onAnswerChange)
        )))
  }
}
