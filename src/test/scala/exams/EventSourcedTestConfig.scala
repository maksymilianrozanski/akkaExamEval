package exams

import akka.persistence.testkit.PersistenceTestKitPlugin
import com.typesafe.config.{Config, ConfigFactory}

object EventSourcedTestConfig {
  val EventSourcedBehaviorTestKitConfigJsonSerialization: Config =
    ConfigFactory.parseString(
      """
      akka {
        actor {
          allow-java-serialization = off
           persistence {
              testkit.events.serialize = on
          }
          serializers {
            jackson-json = "akka.serialization.jackson.JacksonJsonSerializer"
          }
          serialization-bindings {
            "exams.JsonSerializable" = jackson-json
          }
        }
      }
      """.stripMargin).withFallback(PersistenceTestKitPlugin.config)
}
