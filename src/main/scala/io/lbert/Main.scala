package io.lbert

import io.lbert.activemq.ActiveMQ.Topic
import zio.{App, IO, ZEnv, ZIO}
import zio.console._

object Main extends App {

  def run(args: List[String]): ZIO[ZEnv, Nothing, Int] = {
    val topic = Topic("test.topic")
    val prog = ConsoleUtils.readFromTopic(topic)

    prog.foldM(
      err => putStrLn(s"Execution failed with error [$err]") *> IO.succeed(1),
      _   => IO.succeed(0)
    )
  }
}
