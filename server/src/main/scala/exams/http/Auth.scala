package exams.http

import akka.http.scaladsl.server.directives.Credentials

object Auth {

  val secretPass = "pass"

  def userPassAuthenticator(credentials: Credentials): Option[String] = {
    credentials match {
      case pass@Credentials.Provided(username) if pass.verify(secretPass) => Some(username)
      case _ => None
    }
  }
}
