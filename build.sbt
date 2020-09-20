name := "akkaExams"

version := "0.1"

scalaVersion := "2.13.3"

val akkaVersion = "2.6.8"
val akkaHttpVersion = "10.2.0"
lazy val leveldbVersion = "0.7"
lazy val leveldbjniVersion = "1.8"
lazy val protobufVersion = "3.6.1"

libraryDependencies ++= Seq(

  "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
  "com.typesafe.akka" %% "akka-persistence-typed" % akkaVersion,
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
  // https://mvnrepository.com/artifact/org.apache.logging.log4j/log4j-slf4j-impl
  "org.apache.logging.log4j" % "log4j-slf4j-impl" % "2.13.3",
  "ch.qos.logback" % "logback-classic" % "1.2.3",

  // local levelDB stores
  "org.iq80.leveldb" % "leveldb" % leveldbVersion,
  "org.fusesource.leveldbjni" % "leveldbjni-all" % leveldbjniVersion,

  // Google Protocol Buffers
  "com.google.protobuf" % "protobuf-java" % protobufVersion,

  "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % Test,
  "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion % Test,
  "org.scalatest" %% "scalatest" % "3.0.8" % Test
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
