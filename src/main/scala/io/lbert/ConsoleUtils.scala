package io.lbert

import io.lbert.activemq.AMQConnection.{AMQCredentials, AMQUrl, Password, Username}
import io.lbert.activemq.ActiveMQ.Topic
import io.lbert.activemq.{AMQConnection, ActiveMQ}
import javax.jms.TextMessage
import zio.blocking.Blocking
import zio.clock.Clock
import zio.console.Console
import zio.duration.Duration
import zio.stream.ZStream
import zio.{IO, Schedule}

object ConsoleUtils {

  val amqConnection = AMQConnection(
    AMQUrl("tcp://localhost:61616"),
    Some(AMQCredentials(
      Username("admin"),
      Password("admin"),
    ))
  )

  def stream(
    topic: Topic,
    f: Int => String, duration: Duration
  )(
    blocking: Blocking.Service,
    console : Console.Service
  ) =
    ZStream
      .fromSchedule(Schedule.spaced(duration))
      .flatMap(i =>
        ZStream.fromEffect(writeToTopic(topic, f(i))(blocking, console))
      )

  def readFromTopic(
    topic  : Topic
  )(
    blocking: Blocking.Service,
    console : Console.Service
  ): IO[ActiveMQ.Error, Unit] =
    ActiveMQ.getConnection(amqConnection).use(conn =>
      ActiveMQ.forConnection(blocking, conn).use(amq =>
        for {
          _    <- console.putStrLn(s"Reading from topic [${topic.topicName}]")
          _ <- amq.consumeTopic(topic).mapM {
            case message: TextMessage => console.putStrLn(s"Got TextMessage [${message.getText}]")
            case message => console.putStrLn(s"Got ${message.getClass.getSimpleName} [$message]")
          }.runDrain
          _   <- console.putStrLn(s"Done consuming topic [${topic.topicName}]")
        } yield ()
      )
    )

  def writeToTopic(
    topic: Topic,
    message: String
  )(
    blocking: Blocking.Service,
    console : Console.Service
  ): IO[ActiveMQ.Error, Unit] =
    ActiveMQ.getConnection(amqConnection).use(conn =>
      ActiveMQ.forConnection(blocking, conn).use(amq =>
        for {
          _   <- console.putStrLn(s"Writing to topic [${topic.topicName}]")
          _   <- amq.produceTopic(topic, message)
          _   <- console.putStrLn(s"Done writing to topic [${topic.topicName}]")
        } yield ()
      )
    )

}
