package exams.http.token

import java.util.concurrent.TimeUnit

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import exams.distributor.ExamDistributor.ExamId
import pdi.jwt.algorithms.JwtHmacAlgorithm
import pdi.jwt.{JwtAlgorithm, JwtClaim, JwtSprayJson}
import spray.json.{DefaultJsonProtocol, RootJsonFormat, enrichAny}

import scala.util.{Failure, Success}

object TokenGenerator extends DefaultJsonProtocol with SprayJsonSupport {

  val algorithm: JwtHmacAlgorithm = JwtAlgorithm.HS256
  implicit val secretKey: SecretKey = SecretKey("very-secret-akka-exams-key")
  case class SecretKey(key: String)

  def createToken(examId: ExamId, expirationDays: Int)(implicit currentTime: () => Long, secretKey: SecretKey): String = {
    val claims = JwtClaim(
      expiration = Some(currentTime() / 1000 + TimeUnit.DAYS.toSeconds(expirationDays)),
      issuedAt = Some(currentTime() / 1000),
      issuer = Some("HelloJwt"),
      content = TokenContent(examId).toJson.compactPrint
    )
    JwtSprayJson.encode(claims, secretKey.key, algorithm) // JWT string
  }

  case class ExamIdToken(expectedId: ExamId, token: String)

  def validateToken(encodedToken: String, expectedId: ExamId)(implicit currentTime: () => Long, secretKey: SecretKey): Either[TokenValidationResult, ValidToken] =
    decodeToken(encodedToken, secretKey)
      .flatMap(isNotExpired)
      .flatMap(hasExpectedId(expectedId)(_))

  private def decodeToken(token: String, secretKey: SecretKey) =
    JwtSprayJson.decode(token, secretKey.key, Seq(algorithm)) match {
      case Success(value) => Right(value)
      case Failure(_) => Left(InvalidToken)
    }

  private def isNotExpired(claims: JwtClaim)(implicit currentTime: () => Long) =
    if (claims.expiration.getOrElse(0L) > (currentTime() / 1000)) Right(claims.content)
    else Left(TokenExpired)

  private def hasExpectedId(expectedId: ExamId)(tokenContent: String) = {
    import spray.json._
    val examId = tokenContent.parseJson.convertTo[TokenContent].examId
    if (expectedId == examId) Right(ValidToken(expectedId))
    else Left(InvalidTokenContent)
  }

  sealed trait TokenValidationResult
  final case class ValidToken(examId: ExamId) extends TokenValidationResult
  case object InvalidToken extends TokenValidationResult
  case object ParsingError extends TokenValidationResult
  case object InvalidTokenContent extends TokenValidationResult
  case object TokenExpired extends TokenValidationResult

  case class TokenContent(examId: ExamId)
  implicit val tokenFormat: RootJsonFormat[TokenContent] = jsonFormat1(TokenContent)

}


