package io.lbert

import io.lbert.activemq.AMQConnection.{AMQCredentials, AMQUrl, Password, Username}
import io.lbert.activemq.ActiveMQ.Topic
import io.lbert.activemq.{AMQConnection, ActiveMQ}
import javax.jms.{Connection, TextMessage}
import zio.console.putStrLn
import zio.console.Console
import zio.duration.Duration
import zio.stream.ZStream
import zio.{Has, Schedule, ZIO, ZLayer}

object ConsoleUtils {

  val amqConnection = AMQConnection(
    AMQUrl("tcp://localhost:61616"),
    Some(AMQCredentials(
      Username("admin"),
      Password("admin"),
    ))
  )

  val amqConnectionLayer: ZLayer[Any, ActiveMQ.Error, Has[Connection]] =
    ZLayer.fromManaged(ActiveMQ.getConnection(amqConnection))

  val withConn: ZLayer[zio.ZEnv, ActiveMQ.Error, Env] =
    (zio.ZEnv.any >>> ActiveMQ.live) ++
      zio.ZEnv.any ++
      amqConnectionLayer

  def stream(topic: Topic, f: Int => String, duration: Duration) = ZStream
    .fromSchedule(Schedule.spaced(duration))
    .flatMap(i =>
      ZStream.fromEffect(ConsoleUtils.writeToTopic(topic, f(i)))
    )

  type Env = zio.ZEnv with ActiveMQ with Has[Connection]

  def readFromTopic(
    topic: Topic
  ): ZIO[Env, ActiveMQ.Error, Unit] =
    for {
      _ <- putStrLn(s"Reading from topic [${topic.topicName}]")
      _ <- ActiveMQ.consumeTopic(topic).mapM {
        case message: TextMessage => putStrLn(s"Got TextMessage [${message.getText}]")
        case message => putStrLn(s"Got ${message.getClass.getSimpleName} [$message]")
    }.runDrain
      _   <- putStrLn(s"Done consuming topic [${topic.topicName}]")
    } yield ()

  def writeToTopic(
    topic: Topic,
    message: String
  ): ZIO[Env, ActiveMQ.Error, Unit] =
    for {
      _   <- putStrLn(s"Writing to topic [${topic.topicName}]")
      _   <- ActiveMQ.produceTopic(topic, message)
      _   <- putStrLn(s"Done writing to topic [${topic.topicName}]")
    } yield ()

}
