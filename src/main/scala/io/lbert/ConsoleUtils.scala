package io.lbert

import io.lbert.activemq.AMQConnection.{AMQCredentials, AMQUrl, Password, Username}
import io.lbert.activemq.ActiveMQ.Topic
import io.lbert.activemq.{AMQConnection, ActiveMQ}
import javax.jms.Connection
import zio.ZIO
import zio.blocking.Blocking
import zio.console.{Console, putStrLn}

object ConsoleUtils {

  val amqConnection = AMQConnection(
    AMQUrl("tcp://localhost:61616"),
    Some(AMQCredentials(
      Username("admin"),
      Password("admin"),
    ))
  )

  val amqManaged = (Blocking.live >>> ActiveMQ.Service.live).build

  def readFromTopic(topic: Topic): ZIO[Console, ActiveMQ.Error, Unit] = amqManaged.use { amq =>
    ActiveMQ.getConnection(amqConnection).use { conn =>
      for {
        _ <- putStrLn(s"Reading from topic [${topic.topicName}]")
        _ <- amq.get.consumeTopic(topic).provide(conn).mapM(message =>
          putStrLn(s"Got Message [$message]")
        ).runDrain
        _ <- putStrLn(s"Done consuming topic [${topic.topicName}]")
      } yield ()
    }
  }

  def writeToTopic(topic: Topic, message: String): ZIO[Console, ActiveMQ.Error, Unit] = amqManaged.use { amq =>
    ActiveMQ.getConnection(amqConnection).use { conn =>
      for {
        _ <- putStrLn(s"Writing to topic [${topic.topicName}]")
        _ <- amq.get.produceTopic(topic, message).provide(conn)
        _ <- putStrLn(s"Done writing to topic [${topic.topicName}]")
      } yield ()
    }
  }

  type WriteEnv = Console with ActiveMQ with Connection

  def writeToTopic2(
    topic: Topic,
    message: String
  ): ZIO[WriteEnv, ActiveMQ.Error, Unit] =
    for {
      amq <- ZIO.environment[ActiveMQ]
      _   <- putStrLn(s"Writing to topic [${topic.topicName}]")
      _   <- amq.get.produceTopic(topic, message)
      _   <- putStrLn(s"Done writing to topic [${topic.topicName}]")
    } yield ()

}
