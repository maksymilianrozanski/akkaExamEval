package exams.http

import akka.actor.testkit.typed.scaladsl.{ActorTestKit, ScalaTestWithActorTestKit}
import akka.actor.typed
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalatest.BeforeAndAfterEach
import org.scalatest.wordspec.AnyWordSpecLike

object HttpUtils {
  class ScalaTestWithActorTestKitSystemRenamed extends ScalaTestWithActorTestKit {
    val systemTyped: typed.ActorSystem[Nothing] = system

    override def afterAll(): Unit = {
      super.afterAll()
    }
  }

  /**
   * trait solving system: ActorSystem[Nothing] name conflict when using ScalaTestWithActorTestKit and ScalatestRouteTest together
   */
  trait ScalaTestWithActorTestKitWithRouteTest extends AnyWordSpecLike with ScalatestRouteTest with BeforeAndAfterEach {
    val tk = new ScalaTestWithActorTestKitSystemRenamed()
    val testKit: ActorTestKit = tk.testKit
    val systemTyped: typed.ActorSystem[Nothing] = tk.systemTyped

    override def afterAll(): Unit = {
      tk.afterAll()
      super.afterAll()
    }

    override def afterEach(): Unit = {
      tk.afterAll()
      super.afterEach()
    }
  }
}
