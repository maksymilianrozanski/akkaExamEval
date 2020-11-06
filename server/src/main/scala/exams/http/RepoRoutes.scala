package exams.http

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Directives.{path, pathPrefix, _}
import akka.http.scaladsl.server.Route
import exams.http.Auth.CredentialsVerifier
import exams.http.RoutesRoot.AllExamResults
import exams.shared.data.HttpRequests.QuestionsSet

object RepoRoutes extends StudentsExamJsonProtocol with SprayJsonSupport {

  def repoRoutes(implicit addingQuestionsSet: QuestionsSet => Unit,
                 requestResults: AllExamResults, auth: CredentialsVerifier): Route = {
    pathPrefix("repo") {
      Route.seal {
        authenticateBasic(realm = "secure site", auth.verify) { user =>
          innerRepoRoutes
        }
      }
    }
  }

  def innerRepoRoutes(implicit addingQuestionsSet: QuestionsSet => Unit,
                      requestResults: AllExamResults): Route = {
    post {
      (path("add") & extractRequest) { _ =>
        addSetToRepo
      }
    } ~ get {
      path("results") {
        requestAllResults
      }
    }
  }

  private def addSetToRepo(implicit addingQuestions: QuestionsSet => Unit): Route =
    entity(as[QuestionsSet]) { set =>
      addingQuestions(set)
      complete(HttpEntity(ContentTypes.`text/plain(UTF-8)`, "requested adding questions set"))
    }

  private def requestAllResults(implicit requestResults: AllExamResults): Route =
    complete(requestResults())
}
