package exams.http.token

import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.Behaviors

object AskWithToken {
  /**
   * @param asked   ref to actor where the request should be sent
   * @param token   user authentication token
   * @param request message sent to asked
   * @param replyTo ref to actor expecting token with response
   * @tparam Asked    Command of asked actor
   * @tparam Response message sent from asked actor
   * @tparam Token    user authentication token
   * @return
   */
  def apply[Asked, Response, Token](asked: ActorRef[Asked])
                                   (token: Token, request: Asked,
                                    replyTo: ActorRef[(Token, Response)]): Behaviors.Receive[Response] = {
    asked ! request
    Behaviors.receiveMessage[Response] {
      response: Response =>
        replyTo ! (token, response)
        Behaviors.stopped
    }
  }
}
