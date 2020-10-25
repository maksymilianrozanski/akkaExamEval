package exams.http

import exams.http.DisplayedState.{maxQuestionsLens2, setIdLens2, studentIdLens2}
import exams.http.ScalaJs.apiEndpoint
import exams.shared.data.HttpRequests.ExamGenerated
import io.circe.generic.auto._
import io.circe.parser.decode
import io.circe.syntax._
import japgolly.scalajs.react.ScalazReact.{ReactS, _}
import japgolly.scalajs.react.component.builder.Builder
import japgolly.scalajs.react.extra.Ajax
import japgolly.scalajs.react.vdom.html_<^.{<, ^, _}
import japgolly.scalajs.react.{ReactEventFromInput, _}

import scala.util.Try

object ExamRequestPageForm {

  def renderExamRequestForm(state: ReactS.Fix[DisplayedState], $: Builder.Step3[Unit, DisplayedState, Unit]#$, s: DisplayedState) = {
    def studentIdStateHandler(s: ReactEventFromInput) =
      state.mod(studentIdLens2.modify(_ => s.target.value))

    def maxQuestionsStateHandler(s: ReactEventFromInput) =
      state.mod(maxQuestionsLens2.modify(_ => Try(Integer.parseInt(s.target.value)).getOrElse(0)))

    def setIdStateHandler(s: ReactEventFromInput) =
      state.mod(setIdLens2.modify(_ => s.target.value))

    def submitRequest(step3: Builder.Step3[Unit, DisplayedState, Unit]#$) = {
      val ajax = Ajax("POST", apiEndpoint + "/student/start2")
        .setRequestContentTypeJson
        .send(step3.state.examRequestPage.get.studentsRequest.asJson.noSpaces).onComplete {
        xhr =>
          xhr.status match {
            case 200 =>
              println("Sent request and received 200 response code")
              println(s"Response: ${xhr.responseText}")
              import ExamSelectable.fromStudentsExam
              val tokenHeader = xhr.getResponseHeader("Access-Token")
              step3.setState(step3.state.copy(status = Success,
                examPage = decode[ExamGenerated](xhr.responseText).toOption
                  .map(it => ExamPage(tokenHeader, it.exam))))
            case x =>
              println(s"Sent request and received $x response code")
              step3.setState(step3.state.copy(status = Failure))
          }
      }
      step3.modState(i => i, ajax.asCallback)
    }

    <.form(
      ^.onSubmit ==> {(_: ReactEventFromInput).preventDefaultCB},
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
}
