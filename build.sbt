//import sbt.Keys.mainClass
//import sbt.enablePlugins
//import com.typesafe.sbt.web.Import.WebKeys._
import sbtcrossproject.CrossPlugin.autoImport.{crossProject, CrossType}

val akkaVersion = "2.6.9"
val akkaHttpVersion = "10.2.0"
lazy val postgresVersion = "42.2.16"

lazy val commonSettings = Seq(
  version := "0.1",
  name := "akkaExams",
  organization := "exams",
  scalaVersion := "2.13.3",
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
)

lazy val server = project
  .settings(commonSettings)
  .settings(
    scalaJSProjects := Seq(client),
    pipelineStages in Assets := Seq(scalaJSPipeline),
    // triggers scalaJSPipeline when using compile or continuous compilation
    compile in Compile := ((compile in Compile) dependsOn scalaJSPipeline).value,
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

      "com.vmunier" %% "scalajs-scripts" % "1.1.4",

      "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % Test,
      "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion % Test,
      "com.typesafe.akka" %% "akka-persistence-testkit" % akkaVersion % Test,
      "org.scalatest" %% "scalatest" % "3.2.0" % Test
    ),
    WebKeys.packagePrefix in Assets := "public/",
    managedClasspath in Runtime += (packageBin in Assets).value,
    (managedClasspath in Runtime) += (packageBin in Compile in Assets).value,
    //    WebKeys.exportedMappings in Assets ++= (for ((file, path) <- (mappings in Assets).value)
    //      yield file -> ((WebKeys.packagePrefix in Assets).value + path)),

    //    sourceDirectories in(Compile, TwirlKeys.compileTemplates) +=
    //      baseDirectory.value.getParentFile / "src/main/twirl/exams/http",

    herokuAppName in Compile := "akkaexams",
    mainClass in Compile := Some("exams.Main")
  )
  .enablePlugins(SbtWeb, SbtTwirl, JavaAppPackaging, DockerPlugin)
  .dependsOn(sharedJvm)

lazy val client = project
  .settings(commonSettings)
  .settings(
    scalaJSUseMainModuleInitializer := true,
    libraryDependencies ++= Seq(
      "org.scala-js" %%% "scalajs-dom" % "1.1.0"
    )
  )
  .enablePlugins(ScalaJSPlugin, ScalaJSWeb)
  .dependsOn(sharedJs)

lazy val shared = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Pure)
  .in(file("shared"))
  .settings(commonSettings)
  .jvmConfigure(_.enablePlugins(JavaAppPackaging, DockerPlugin))
  .jsConfigure(_.enablePlugins(ScalaJSWeb))

lazy val sharedJvm = shared.jvm
lazy val sharedJs = shared.js




