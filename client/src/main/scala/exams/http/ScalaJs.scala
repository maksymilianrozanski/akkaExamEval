package exams.http

import org.scalajs.dom
import org.scalajs.dom.html.Div
import japgolly.scalajs.react.vdom.html_<^._

object ScalaJs {

  val apiEndpoint = "localhost:8080"

  def main(args: Array[String]): Unit = {
    val root = dom.document.getElementById("scalajsShoutOut")

    val examForm = requestExamForm()
    examForm.renderIntoDOM(root)
  }

  def requestExamForm(): VdomTagOf[Div] = {
    <.div(
      <.label(
        ^.`for` := "studentId",
        "Student Id"),
      <.input(
        ^.`type` := "text",
        ^.name := "studentId",
        ^.id := "studentId"
      ),
      <.label(
        ^.`for` := "maxQuestions",
        "max. questions"
      ),
      <.input(
        ^.`type` := "number",
        ^.name := "maxQuestions",
        ^.id := "maxQuestions"
      ),
      <.label(
        ^.`for` := "setId",
        "set id"
      ),
      <.input(
        ^.`type` := "text",
        ^.name := "setId",
        ^.id := "setId"
      ),
      <.button(
        ^.id := "examClicked",
        "Request"
      ))
  }
}
