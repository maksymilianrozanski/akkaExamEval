package exams.http

import exams.shared.SharedMessages
import org.scalajs.dom

object ScalaJs {

  def main(args: Array[String]): Unit = {
    dom.document.getElementById("scalajsShoutOut").textContent = SharedMessages.itWorks + "!!!"

    val button = dom.document.createElement("button")
    button.innerHTML = "Hi"

    button.addEventListener("click", {
      event: dom.MouseEvent => addText()
    })

    dom.document.getElementById("scalajsShoutOut").appendChild(button)

    def printHi(): Unit =
      println("Hi!")

    def addText(): Unit = {
      val divElement = dom.document.createElement("div")
      divElement.innerHTML = "Hi.!"
      dom.document.getElementById("scalajsShoutOut").appendChild(divElement)
    }
  }
}
