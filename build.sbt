import Dependencies._

lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization := "com.example",
      scalaVersion := "2.12.7",
      version      := "0.1.0-SNAPSHOT"
    )),
    name := "zio-amq",
    libraryDependencies ++= Seq(
      zio,
      zioStreams,
      zioLogging,
      activemq,
      scalaTest % Test
    ),
    scalacOptions ++= Seq(
      "-encoding", "utf8", // Option and arguments on same line
      "-Xfatal-warnings",  // New lines for each options
      "-deprecation",
      "-unchecked",
      "-language:implicitConversions",
      "-language:higherKinds",
      "-language:existentials",
      "-language:postfixOps"
    ),
    resolvers += Resolver.sonatypeRepo("releases"),
    addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.8")
  )
