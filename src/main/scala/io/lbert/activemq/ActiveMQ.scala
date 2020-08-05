package io.lbert.activemq

import javax.jms._
import org.apache.activemq.ActiveMQConnectionFactory
import zio.blocking.Blocking
import zio.stream.{ZStream, Stream}
import zio._
import ActiveMQ.Error

trait ActiveMQ {
  def consumeTopic(topic: ActiveMQ.Topic): Stream[Error, Message]
  def produceTopic(topic: ActiveMQ.Topic, message: String): IO[Error, Unit]
}

object ActiveMQ {

  sealed trait Error
  object Error {
    final case class ConnectionError(t: Throwable) extends Error
    final case class SessionError(t: Throwable) extends Error
    final case class ConsumerError(t: Throwable) extends Error
    final case class ProducerError(t: Throwable) extends Error
    final case class UnsupportedMessage(message: Message) extends Error
  }
  import Error._

  def forConnection(
    blocking  : Blocking.Service,
    connection: Connection
  ): UManaged[ActiveMQ] = ZManaged.succeed(new ActiveMQ {

    override def consumeTopic(
      topic: Topic
    ): Stream[ActiveMQ.Error, Message] =
      for {
        sess <- ZStream.managed(getSession(connection))
        cons <- ZStream.managed(getConsumer(sess, topic))
        out  <- ZStream.effectAsyncM[Any, Error, Message] { offer =>
          blocking.effectBlocking {
            cons.setMessageListener(new MessageListener {
              override def onMessage(message: Message): Unit = {
                println("Listening for a message, but just sleeping......")
                Thread.sleep(Long.MaxValue)
                offer(ZIO.succeed(Chunk(message)))
              }
            })
          }.mapError(ConsumerError)
        }.ensuring(UIO(blocking.effectBlocking(cons.close())))
      } yield out

    override def produceTopic(
      topic  : Topic,
      message: String
    ): IO[ActiveMQ.Error, Unit] =
      getSession(connection).use { sess =>
        getProducer(sess, topic).use { prod =>
          blocking.effectBlocking {
            prod.send(sess.createTextMessage(message))
          }.mapError(ProducerError)
        }
      }
  })

  def getConnection(connection: AMQConnection): Managed[Error, Connection] =
    IO.effect {
      val con =
        connection.credentials match {
          case None => new ActiveMQConnectionFactory(connection.url.url).createConnection()
          case Some(c) => new ActiveMQConnectionFactory(connection.url.url).createConnection(c.user.user, c.pass.pass)
        }
      con.setExceptionListener(new ExceptionListener {
        override def onException(exception: JMSException): Unit = {
          println(s"Got a fun JMS exception [$exception]")
        }
      })
      con.start()
      con
    }.mapError(ConnectionError).toManaged(c => UIO(c.close()))

  def getSession(
    connection: Connection,
    config: AMQSession.Config = AMQSession.Config.default
  ): Managed[Error, Session] =
    IO.effect(connection.createSession(config.transacted, config.acknowledgeMode.modeInt))
      .mapError(SessionError)
      .toManaged(s => UIO(s.close()))

  def getConsumer(session: Session, topic: Topic): Managed[Error, MessageConsumer] =
    IO.effect(session.createConsumer(session.createTopic(topic.topicName)))
      .mapError(ConsumerError)
      .toManaged(c => UIO(c.close()))

  def getProducer(session: Session, topic: Topic): Managed[Error, MessageProducer] =
    IO.effect(session.createProducer(session.createTopic(topic.topicName)))
      .mapError(ProducerError)
      .toManaged(c => UIO(c.close()))

  final case class Topic(topicName: String)
  final case class Queue(topicName: String)
}
