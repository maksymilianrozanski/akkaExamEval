name := "akkaExams"

version := "0.1"

scalaVersion := "2.13.3"

val AkkaVersion = "2.6.8"

libraryDependencies += "com.typesafe.akka" %% "akka-actor-typed" % AkkaVersion
// https://mvnrepository.com/artifact/org.apache.logging.log4j/log4j-slf4j-impl
libraryDependencies += "org.apache.logging.log4j" % "log4j-slf4j-impl" % "2.13.3"
libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.3"
