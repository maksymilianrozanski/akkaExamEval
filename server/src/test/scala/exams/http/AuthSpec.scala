package exams.http

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.BasicHttpCredentials
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

class AuthSpec extends AnyWordSpecLike with ScalatestRouteTest with Matchers {

  "userPassAuthenticator" when {

    val route = Route.seal {
      path("secured") {
        authenticateBasic(realm = "secure site", Auth.userPassAuthenticator) { username =>
          complete(s"hello, username is $username")
        }
      }
    }

    val validPassword = Auth.secretPass
    "valid password provided" should {
      "return content" in {
        Get("/secured") ~> addCredentials(BasicHttpCredentials("john", validPassword)) ~> route ~> check {
          responseAs[String] shouldEqual s"hello, username is john"
          status shouldBe StatusCodes.OK
        }
      }
    }

    "no credentials provided" should {
      "not provide access" in {
        Get("/secured") ~> route ~> check {
          status shouldBe StatusCodes.Unauthorized
        }
      }
    }

    "invalid password provided" should {
      "not provide access" in {
        Get("/secured") ~> addCredentials(BasicHttpCredentials(s"invalid$validPassword")) ~> route ~> check {
          status shouldBe StatusCodes.Unauthorized
        }
      }
    }
  }
}
