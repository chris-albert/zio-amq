package io.lbert

import java.util.concurrent.TimeUnit
import io.lbert.activemq.ActiveMQ
import io.lbert.activemq.ActiveMQ.Topic
import zio.clock._
import zio.console.Console
import zio.blocking.Blocking
import zio.duration.Duration
import zio.{App, ExitCode, ZEnv, ZIO}

object Main extends App {

  val topic = Topic("test.topic")

  def program: ZIO[zio.ZEnv, ActiveMQ.Error, Unit] =
    for {
      blocking <- ZIO.environment[Blocking].map(_.get)
      console  <- ZIO.environment[Console].map(_.get)
      clock    <- ZIO.environment[Clock].map(_.get)
      _        <- ConsoleUtils.readFromTopic(topic)(blocking, console).fork
      _        <- clock.sleep(Duration(1, TimeUnit.SECONDS))
      _        <- ConsoleUtils.stream(topic, i => s"Heyo there... [$i]", Duration(1, TimeUnit.SECONDS))(blocking, console)
                    .take(5)
                    .runDrain
    } yield ()

  def run(args: List[String]): ZIO[ZEnv, Nothing, ExitCode] =
    program.exitCode
}
