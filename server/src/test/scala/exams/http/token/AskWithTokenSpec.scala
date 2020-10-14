package exams.http.token

import akka.actor.testkit.typed.scaladsl.{BehaviorTestKit, TestInbox}
import org.scalatest.wordspec.AnyWordSpecLike

class AskWithTokenSpec extends AnyWordSpecLike {

  "AskWithToken" when {
    case class AskedCommand(content: String)
    case class Response(content: Int)
    case class Token(tokenContent: String)
    val token = Token("1x2z")
    val request = AskedCommand("request content")
    "receive the token and request" should {
      val asked = TestInbox[AskedCommand]()
      val asking = TestInbox[(Token, Response)]()
      val testKit = BehaviorTestKit(AskWithToken(asked.ref)(token, request, asking.ref))
      "send the message to asked" in {
        asked.expectMessage(request)
      }
    }

    "receive the response from asked" should {
      val asked = TestInbox[AskedCommand]()
      val asking = TestInbox[(Token, Response)]()
      val testKit = BehaviorTestKit(AskWithToken(asked.ref)(token, request, asking.ref))
      testKit.run(Response(42))
      "send message with token to asking" in {
        asking.expectMessage((token, Response(42)))
      }
    }
  }
}
