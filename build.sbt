import sbtcrossproject.CrossPlugin.autoImport.{CrossType, crossProject}

val akkaVersion = "2.6.9"
val akkaHttpVersion = "10.2.0"
lazy val postgresVersion = "42.2.16"
lazy val scalaTestVersion = "3.2.0"

onLoad in Global := (onLoad in Global).value andThen (Command.process("project server", _))

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
      "org.scalatest" %% "scalatest" % scalaTestVersion % Test
    ),
    WebKeys.packagePrefix in Assets := "public/",
    managedClasspath in Runtime += (packageBin in Assets).value,
    (managedClasspath in Runtime) += (packageBin in Compile in Assets).value,
    WebKeys.exportedMappings in Assets ++= (for ((file, path) <- (mappings in Assets).value)
      yield file -> ((WebKeys.packagePrefix in Assets).value + path)),

    herokuAppName in Compile := "akkaexams",
    mainClass in Compile := Some("exams.Main")
  )
  .enablePlugins(SbtWeb, SbtTwirl, JavaAppPackaging, DockerPlugin)
  .dependsOn(sharedJvm)


val sprayVersion = "1.3.5-7"
val scalaJSReact = "1.7.5"
val scalaCss = "0.6.1"
val reactJS = "16.13.1"
val circeVersion = "0.14.0-M1"
lazy val client = project
  .settings(commonSettings)
  .settings(
    scalaJSUseMainModuleInitializer := true,
    jsEnv := new org.scalajs.jsenv.jsdomnodejs.JSDOMNodeJSEnv(),
    name := "client",
    libraryDependencies ++= Seq(
      "org.scala-js" %%% "scalajs-dom" % "1.1.0",
      "com.github.japgolly.scalajs-react" %%% "core" % scalaJSReact,
      "com.github.japgolly.scalajs-react" %%% "extra" % scalaJSReact,
      "com.github.japgolly.scalacss" %%% "core" % scalaCss,
      "com.github.japgolly.scalacss" %%% "ext-react" % scalaCss,
      "com.github.japgolly.scalajs-react" %%% "ext-scalaz72" % "1.7.5",
      "io.circe" %%% "circe-core" % circeVersion,
      "io.circe" %%% "circe-generic" % circeVersion,
      "io.circe" %%% "circe-parser" % circeVersion,
      "com.github.julien-truffaut" %%% "monocle-core" % "2.0.3",
      "com.github.julien-truffaut" %%% "monocle-macro" % "2.0.3",
      "org.scalatest" %%% "scalatest" % scalaTestVersion % Test,
      "com.github.japgolly.scalajs-react" %%% "test" % "1.7.5" % Test
    ),

    // creates single js resource file for easy integration in html page
    skip in packageJSDependencies := false,

    // copy  javascript files to js folder,that are generated using fastOptJS/fullOptJS
    crossTarget in(Compile, fullOptJS) := file("js"),
    crossTarget in(Compile, fastOptJS) := file("js"),
    crossTarget in(Compile, packageJSDependencies) := file("js"),
    crossTarget in(Compile, packageMinifiedJSDependencies) := file("js"),
    artifactPath in(Compile, fastOptJS) := ((crossTarget in(Compile, fastOptJS)).value /
      ((moduleName in fastOptJS).value + "-opt.js")),
    scalacOptions += "-feature",

    jsDependencies ++= Seq(
      "org.webjars.npm" % "react" % reactJS
        / "umd/react.development.js"
        minified "umd/react.production.min.js"
        commonJSName "React",
      "org.webjars.npm" % "react-dom" % reactJS
        / "umd/react-dom.development.js"
        minified "umd/react-dom.production.min.js"
        dependsOn "umd/react.development.js"
        commonJSName "ReactDOM",
      "org.webjars.npm" % "react-dom" % reactJS
        / "umd/react-dom-server.browser.development.js"
        minified "umd/react-dom-server.browser.production.min.js"
        dependsOn "umd/react-dom.development.js"
        commonJSName "ReactDOMServer",
      "org.webjars.npm" % "react-dom" % reactJS % Test
        / "umd/react-dom-test-utils.development.js"
        minified "umd/react-dom-test-utils.production.min.js"
        dependsOn "umd/react-dom.development.js"
        commonJSName "ReactTestUtils"
    )

    //enablePlugins(ScalaJSBundlerPlugin)
    //npmDependencies in Compile ++= Seq(
    //)
    // fixes unresolved deps issue: https://github.com/webjars/webjars/issues/1789
    //    dependencyOverrides ++= Seq(
    //      "org.webjars.npm" % "js-tokens" % "4.0.0",
    //      "org.webjars.npm" % "scheduler" % "0.14.0"
    //    )
  )
  .enablePlugins(ScalaJSPlugin, ScalaJSWeb, JSDependenciesPlugin)
  .dependsOn(sharedJs)

lazy val shared = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Pure)
  .in(file("shared"))
  .settings(commonSettings, name := "examsShared")
  .jvmConfigure(_.enablePlugins(JavaAppPackaging, DockerPlugin))
  .jsConfigure(_.enablePlugins(ScalaJSWeb))

lazy val sharedJvm = shared.jvm
lazy val sharedJs = shared.js
