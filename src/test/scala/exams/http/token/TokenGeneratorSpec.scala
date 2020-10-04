package exams.http.token

import exams.http.token.TokenGenerator.{ValidToken, decodeToken}
import org.scalatest.wordspec.AnyWordSpecLike

class TokenGeneratorSpec extends AnyWordSpecLike {

  "TokenGenerator" should {
    val someDay = 1601807051252L
    val token = TokenGenerator.createToken("exam123", 7)(() => someDay)
    "decode previously encoded token" in {
      val result = TokenGenerator.validateToken(token, "exam123")(() => someDay + 39000)
      assertResult(Right(ValidToken("exam123")))(result)
    }
  }

  "TokenGenerator" when {
    "was not able to decode token" should {
      "return Left(InvalidToken)" in {

      }
    }

    "was not able to parse token content" should {
      "return Left(ParsingError)" in {

      }
    }

    "examId of token does not match to expected" should {
      "return Left(InvalidTokenContent)" in {

      }
    }

    "token expiration date passed" should {
      "return Left(TokenExpired)" in {

      }
    }
  }
}
