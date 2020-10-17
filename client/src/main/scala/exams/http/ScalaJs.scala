package exams.http

import exams.shared.SharedMessages
import org.scalajs.dom
import org.scalajs.dom.raw.{MouseEvent, Node}
import org.scalajs.dom.{Document, html}

object ScalaJs {

  def main(args: Array[String]): Unit = {
    implicit val doc: html.Document = dom.document
    val root = dom.document.getElementById("scalajsShoutOut")

    dom.document.getElementById("scalajsShoutOut").textContent = SharedMessages.itWorks + "!!!"

    def addText(): Unit = {
      val divElement = dom.document.createElement("div")
      divElement.innerHTML = "Hi.!"
      dom.document.getElementById("scalajsShoutOut").appendChild(divElement)
    }

    val examForm = requestExamForm()
    root.appendChild(examForm)

    val examClickedButton = doc.getElementById("examClicked")
    examClickedButton.addEventListener("click", (event: MouseEvent) => addText())
  }

  def requestExamForm()(implicit document: Document): Node = {
    val element = document.createElement("div")
    element.innerHTML =
      s"""
    <label for='studentId'>Student id</label>
    <input type='text' name='studentId' id='studentId'>
    <label for='maxQuestions'>max. questions</label>
    <input type='number' name='maxQuestions' id='maxQuestions'>
    <label for='setId'>set id</label>
    <input type='text' name='setId' id='setId'>
    <button id='examClicked'>Request</button>
    """.stripMargin
    element
  }

}
