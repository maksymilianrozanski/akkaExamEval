package exams.http.token


import java.util.concurrent.TimeUnit

import exams.http.token.TokenGenerator.{InvalidToken, InvalidTokenContent, ParsingError, SecretKey, ValidToken, decodeToken}
import org.scalatest.wordspec.AnyWordSpecLike
import pdi.jwt.{JwtAlgorithm, JwtClaim, JwtSprayJson}

class TokenGeneratorSpec extends AnyWordSpecLike {

  private val someDay = 1601807051252L
  private val oneHourLater = someDay + 3600 * 1000
  private implicit val secretKey: SecretKey = SecretKey("unit test secret key")
  "TokenGenerator" should {
    val token = TokenGenerator.createToken("exam123", 7)(() => someDay, secretKey)
    "decode previously encoded token" in {
      val result = TokenGenerator.validateToken(token, "exam123")(() => someDay + 39000, secretKey)
      assertResult(Right(ValidToken("exam123")))(result)
    }
  }

  "TokenGenerator" when {
    "was not able to decode token" should {
      val tokenWithOtherKey = TokenGenerator.createToken("exam123", 7)(() => someDay, SecretKey("other-key"))
      "return Left(InvalidToken)" in {
        assertResult(Left(InvalidToken))(
          TokenGenerator.validateToken(tokenWithOtherKey, "exam123")(
            () => oneHourLater, SecretKey("valid-key")))
      }
    }

    "was not able to parse token content" should {
      val otherClaim = JwtClaim(
        expiration = Some(someDay / 1000 + TimeUnit.DAYS.toSeconds(7)),
        issuedAt = Some(someDay / 1000),
        issuer = Some("Hello"),
        content =
          """
             {"someValue" : "someText"}
             """.stripMargin
      )
      val someKey = "unit-test-key"
      val invalidToken = JwtSprayJson.encode(otherClaim, someKey, TokenGenerator.algorithm)

      "return Left(ParsingError)" in {
        assertResult(Left(ParsingError))(TokenGenerator.validateToken(invalidToken,
          "someText")(() => oneHourLater, SecretKey(someKey)))
      }
    }

    "examId of token does not match to expected" should {
      val token = TokenGenerator.createToken("exam2", 7)(() => someDay, secretKey)

      "return Left(InvalidTokenContent)" in {
        assertResult(Left(InvalidTokenContent))(
          TokenGenerator.validateToken(token, "exam12345")(() => oneHourLater, secretKey))
      }
    }

    "token expiration date passed" should {
      "return Left(TokenExpired)" in {

      }
    }
  }
}
