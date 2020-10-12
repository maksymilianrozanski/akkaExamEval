name := "akkaExams"

version := "0.1"

scalaVersion := "2.13.3"

val akkaVersion = "2.6.9"
val akkaHttpVersion = "10.2.0"
lazy val postgresVersion = "42.2.16"

libraryDependencies ++= Seq(

  "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
  "com.typesafe.akka" %% "akka-persistence-typed" % akkaVersion,
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.typesafe.akka" %% "akka-serialization-jackson" % akkaVersion,
  "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
  "com.pauldijou" %% "jwt-spray-json" % "4.3.0",
  // https://mvnrepository.com/artifact/org.apache.logging.log4j/log4j-slf4j-impl
  "org.apache.logging.log4j" % "log4j-slf4j-impl" % "2.13.3",
  "ch.qos.logback" % "logback-classic" % "1.2.3",

  "org.postgresql" % "postgresql" % postgresVersion,
  "com.github.dnvriend" %% "akka-persistence-jdbc" % "3.5.2",

  "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % Test,
  "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion % Test,
  "com.typesafe.akka" %% "akka-persistence-testkit" % akkaVersion % Test,
  "org.scalatest" %% "scalatest" % "3.2.0" % Test
)

scalacOptions ++= Seq(
  "-encoding", "utf8", // Option and arguments on same line
  "-Xfatal-warnings", // New lines for each options
  "-deprecation",
  "-unchecked",
  "-Xcheckinit",
  "-language:implicitConversions",
  "-language:higherKinds",
  "-language:existentials",
  "-language:postfixOps"
)

herokuAppName in Compile := "akkaexams"
mainClass in Compile := Some("exams.Main")
enablePlugins(JavaAppPackaging)
enablePlugins(DockerPlugin)
