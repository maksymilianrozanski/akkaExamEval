package exams.http

import akka.http.scaladsl.server.directives.Credentials

object Auth {

  private[http] def userPassAuthenticator(validPass: Option[String])(credentials: Credentials): Option[String] = {
    if (validPass.isEmpty) println("adminPass from environment is missing!")
    validPass.flatMap(validPass =>
      credentials match {
        case pass@Credentials.Provided(username) if pass.verify(validPass) => Some(username)
        case _ => None
      }
    )
  }

  implicit def adminCredentials: CredentialsVerifier = CredentialsVerifier(sys.env.get("adminPass"))

  final case class CredentialsVerifier(validPassword: Option[String]) {
    def verify(credentials: Credentials): Option[String] = userPassAuthenticator(validPassword)(credentials)
  }
}
