package exams.http

import akka.http.scaladsl.server.directives.Credentials

import scala.io.Source

object Auth {

  val secretPass: String = Source.fromResource("repoAuth.txt").getLines().toList.head

  def userPassAuthenticator(credentials: Credentials): Option[String] = {
    credentials match {
      case pass@Credentials.Provided(username) if pass.verify(secretPass) => Some(username)
      case _ => None
    }
  }
}
