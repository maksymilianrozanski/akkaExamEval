package exams.http

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Directives.{path, pathPrefix, _}
import akka.http.scaladsl.server.Route
import exams.data.ExamRepository.QuestionsSet

object RepoRoutes extends StudentsExamJsonProtocol with SprayJsonSupport {

  def repoRoutes(implicit addingQuestionsSet: QuestionsSet => Unit): Route = {
    pathPrefix("repo") {
      Route.seal {
        authenticateBasic(realm = "secure site", Auth.userPassAuthenticator) { user =>
          innerRepoRoutes
        }
      }
    }
  }

  def innerRepoRoutes(implicit addingQuestionsSet: QuestionsSet => Unit): Route =
    (path("add") & post & extractRequest) { _ =>
      addSetToRepo
    }

  private def addSetToRepo(implicit addingQuestions: QuestionsSet => Unit): Route =
    entity(as[QuestionsSet]) { set =>
      addingQuestions(set)
      complete(HttpEntity(ContentTypes.`text/plain(UTF-8)`, "requested adding questions set"))
    }
}
