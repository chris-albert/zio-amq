package io.lbert

import io.lbert.activemq.AMQConnection.{AMQCredentials, AMQUrl, Password, Username}
import io.lbert.activemq.ActiveMQ.Topic
import io.lbert.activemq.{AMQConnection, ActiveMQ}
import zio.blocking.Blocking
import zio.console.putStrLn

object ConsoleUtils {

  val amqConnection = AMQConnection(
    AMQUrl("tcp://localhost:61616"),
    Some(AMQCredentials(
      Username("admin"),
      Password("admin"),
    ))
  )

  val amqManaged = (Blocking.live >>> ActiveMQ.Service.live).build

  def readFromTopic(topic: Topic) = amqManaged.use { amq =>
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

  def writeToTopic(topic: Topic, message: String) = amqManaged.use { amq =>
    ActiveMQ.getConnection(amqConnection).use { conn =>
      for {
        _ <- putStrLn(s"Writing to topic [${topic.topicName}]")
        _ <- amq.get.produceTopic(message, topic).provide(conn)
        _ <- putStrLn(s"Done writing to topic [${topic.topicName}]")
      } yield ()
    }
  }

}
