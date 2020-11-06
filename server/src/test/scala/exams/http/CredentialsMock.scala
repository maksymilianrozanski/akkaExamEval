package exams.http

import exams.http.Auth.CredentialsVerifier

object CredentialsMock {

  def setPassword(validPassword: String): CredentialsVerifier = CredentialsVerifier(Some(validPassword))

}
