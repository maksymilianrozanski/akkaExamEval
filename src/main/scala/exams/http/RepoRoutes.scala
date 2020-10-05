package exams.http

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Directives.{path, pathPrefix, _}
import akka.http.scaladsl.server.Route
import exams.data.ExamRepository.QuestionsSet
import exams.http.RoutesRoot.AllExamResults

object RepoRoutes extends StudentsExamJsonProtocol with SprayJsonSupport {

  def repoRoutes(implicit addingQuestionsSet: QuestionsSet => Unit,
                 requestResults: AllExamResults): Route = {
    pathPrefix("repo") {
      Route.seal {
        authenticateBasic(realm = "secure site", Auth.userPassAuthenticator) { user =>
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
