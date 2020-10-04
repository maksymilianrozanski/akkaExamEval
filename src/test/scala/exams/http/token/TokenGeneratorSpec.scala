package exams.http.token

import java.util.concurrent.TimeUnit

import exams.http.token.TokenGenerator.{InvalidToken, InvalidTokenContent, ParsingError, SecretKey, TokenExpired, ValidToken}
import org.scalatest.wordspec.AnyWordSpecLike
import pdi.jwt.{JwtClaim, JwtSprayJson}

class TokenGeneratorSpec extends AnyWordSpecLike {

  private val someDay = 1601807051252L
  private val oneHourLaterTime = () => someDay + TimeUnit.HOURS.toMillis(1)
  private implicit val stubCurrentTime: () => Long = () => someDay
  private implicit val secretKey: SecretKey = SecretKey("unit test secret key")

  "TokenGenerator" should {
    val token = TokenGenerator.createToken("exam123", 7)
    "decode previously encoded token" in {
      val result = TokenGenerator.validateToken(token, "exam123")(oneHourLaterTime, secretKey)
      assertResult(Right(ValidToken("exam123")))(result)
    }
  }

  "TokenGenerator" when {
    "was not able to decode token" should {
      val tokenWithOtherKey = TokenGenerator.createToken("exam123", 7)(stubCurrentTime, SecretKey("other-key"))
      "return Left(InvalidToken)" in {
        assertResult(Left(InvalidToken))(
          TokenGenerator.validateToken(tokenWithOtherKey, "exam123")(
            oneHourLaterTime, secretKey))
      }
    }

    "was not able to parse token content" should {
      val otherClaim = JwtClaim(
        expiration = Some(someDay / 1000 + TimeUnit.DAYS.toSeconds(7)),
        issuedAt = Some(someDay / 1000),
        issuer = Some("SomeIssuer"),
        content =
          """
             {"someValue" : "someText"}
             """.stripMargin)
      val someKey = "unit-test-key"
      val invalidToken = JwtSprayJson.encode(otherClaim, someKey, TokenGenerator.algorithm)

      "return Left(ParsingError)" in
        assertResult(Left(ParsingError))(TokenGenerator.validateToken(invalidToken,
          "someText")(oneHourLaterTime, SecretKey(someKey)))
    }

    "examId of token does not match to expected" should {
      val token = TokenGenerator.createToken("exam2", 7)
      "return Left(InvalidTokenContent)" in
        assertResult(Left(InvalidTokenContent))(
          TokenGenerator.validateToken(token, "exam12345")(oneHourLaterTime, secretKey))
    }

    "token expiration date passed" should {
      val token = TokenGenerator.createToken("exam123", 7)
      val tenDaysLater = someDay + TimeUnit.DAYS.toMillis(10)
      "return Left(TokenExpired)" in
        assertResult(Left(TokenExpired))(
          TokenGenerator.validateToken(token, "exam123")(() => tenDaysLater, secretKey))
    }
  }
}
