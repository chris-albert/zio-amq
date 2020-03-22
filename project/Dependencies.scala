import sbt._

object Dependencies {
  lazy val scalaTest  = "org.scalatest" %% "scalatest"   % "3.0.5"

  //ZIO
  lazy val zioVersion =  "1.0.0-RC18-2"

  lazy val zio        = "dev.zio" %% "zio" % zioVersion
  lazy val zioStreams = "dev.zio" %% "zio-streams" % zioVersion
  lazy val zioLogging = "dev.zio" %% "zio-logging" % "0.1.0"

  lazy val activemq = "org.apache.activemq" % "activemq-client" % "5.15.11"
}
