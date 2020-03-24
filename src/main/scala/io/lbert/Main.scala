package io.lbert

import java.util.concurrent.TimeUnit
import io.lbert.activemq.ActiveMQ.Topic
import zio.clock._
import zio.console._
import zio.duration.Duration
import zio.{App, IO, ZEnv, ZIO}

object Main extends App {

  def run(args: List[String]): ZIO[ZEnv, Nothing, Int] = {
    val topic = Topic("test.topic")
    val prog = (for {
      _ <- ConsoleUtils.readFromTopic(topic).fork
      _ <- sleep(Duration(1, TimeUnit.SECONDS))
      _ <- ConsoleUtils.stream(topic, i => s"Heyo there... [$i]", Duration(1, TimeUnit.SECONDS))
        .take(5)
        .runDrain
    } yield ())
      .provideLayer(ConsoleUtils.withConn)

    prog.foldM(
      err => putStrLn(s"Execution failed with error [$err]") *> IO.succeed(1),
      _   => IO.succeed(0)
    )
  }
}
