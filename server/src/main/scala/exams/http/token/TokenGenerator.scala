package exams.http.token

import java.util.concurrent.TimeUnit

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import exams.distributor.ExamDistributor.{ExamId, emptyState, examAddedHandler}
import pdi.jwt.algorithms.JwtHmacAlgorithm
import pdi.jwt.{JwtAlgorithm, JwtClaim, JwtOptions, JwtSprayJson}
import spray.json.{DefaultJsonProtocol, RootJsonFormat, enrichAny}

import scala.util.{Failure, Success, Try}

object TokenGenerator extends DefaultJsonProtocol with SprayJsonSupport {

  val algorithm: JwtHmacAlgorithm = JwtAlgorithm.HS256
  implicit val secretKey: SecretKey = SecretKey("very-secret-akka-exams-key")
  case class SecretKey(key: String)

  def createToken(examId: ExamId, expirationDays: Int)(implicit currentTime: () => Long, secretKey: SecretKey): String = {
    val claims = JwtClaim(
      expiration = Some((currentTime() / 1000) + TimeUnit.DAYS.toSeconds(expirationDays)),
      issuedAt = Some(currentTime() / 1000),
      content = TokenContent(examId).toJson.compactPrint
    )
    JwtSprayJson.encode(claims, secretKey.key, algorithm) // JWT string
  }

  def validateToken(encodedToken: String, expectedId: ExamId)(implicit currentTime: () => Long, secretKey: SecretKey): Either[TokenValidationResult, ValidMatchedToken] =
    decodeToken(encodedToken, secretKey)
      .flatMap(isNotExpired)
      .flatMap(hasExpectedId(expectedId)(_))

  private def decodeToken(token: String, secretKey: SecretKey) =
    JwtSprayJson.decode(token, secretKey.key, Seq(algorithm), JwtOptions.DEFAULT.copy(expiration = false)) match {
      case Success(value) => Right(value)
      case Failure(_) => Left(InvalidToken)
    }

  private def isNotExpired(claims: JwtClaim)(implicit currentTime: () => Long) =
    if (claims.expiration.getOrElse(0L) > (currentTime() / 1000)) Right(claims.content)
    else Left(TokenExpired)

  private def hasExpectedId(expectedId: ExamId)(tokenContent: String): Either[TokenValidationResult, ValidMatchedToken] = {
    import spray.json._
    Try(tokenContent.parseJson.convertTo[TokenContent].examId) match {
      case Success(examId) =>
        if (expectedId == examId) Right(ValidMatchedToken(expectedId)) else Left(InvalidTokenContent)
      case Failure(_: DeserializationException) =>
        Left(ParsingError)
      case Failure(e) => throw e
    }
  }

  sealed trait TokenValidationResult
  final case class ValidMatchedToken(examId: ExamId) extends TokenValidationResult
  /**
   * was not able to decode the token
   */
  case object InvalidToken extends TokenValidationResult
  /**
   * error during deserialization - expected content matching TokenContent case class as JSON
   */
  case object ParsingError extends TokenValidationResult
  /**
   * token not matching ExamId from request
   */
  case object InvalidTokenContent extends TokenValidationResult
  case object TokenExpired extends TokenValidationResult

  case class TokenContent(examId: ExamId)
  implicit val tokenFormat: RootJsonFormat[TokenContent] = jsonFormat1(TokenContent)

}


