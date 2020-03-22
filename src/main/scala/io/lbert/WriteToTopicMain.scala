package io.lbert

import io.lbert.activemq.ActiveMQ.Topic
import zio.console.putStrLn
import zio.{App, IO, ZIO}

object WriteToTopicMain extends App {


  override def run(args: List[String]): ZIO[zio.ZEnv, Nothing, Int] = {
    val topic = Topic("test.topic")
    val prog = ConsoleUtils.writeToTopic(topic, "testing... 1... 2... 3... ")

    prog.foldM(
      err => putStrLn(s"Execution failed with error [$err]") *> IO.succeed(1),
      _   => IO.succeed(0)
    )
  }

}
