package io.lbert

import javax.jms._
import org.apache.activemq.ActiveMQConnectionFactory
import zio.blocking.Blocking
import zio.stream.ZStream
import zio._

package object activemq {

  type ActiveMQ = Has[ActiveMQ.Service]

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

    trait Service {
      def consumeTopic(topic: Topic): ZStream[Connection, Error, Message]
      def produceTopic(topic: Topic, message: String): ZIO[Connection, Error, Unit]
    }

    object Service {
      val live: ZLayer[Blocking, Nothing, ActiveMQ] = ZLayer.fromFunction { blocking =>
         new Service {
           override def consumeTopic(topic: Topic): ZStream[Connection, Error, Message] =
             for {
               conn <- ZStream.environment[Connection]
               sess <- ZStream.managed(getSession(conn))
               cons <- ZStream.managed(getConsumer(sess, topic))
               out  <- ZStream.effectAsyncM[Any, Error, Message] { offer =>
                 blocking.get.effectBlocking {
                   cons.setMessageListener(new MessageListener {
                     override def onMessage(message: Message): Unit =
                       offer(ZIO.succeed(message))
                   })
                 }.mapError(ConsumerError)
               }.ensuring(UIO(blocking.get.effectBlocking(cons.close())))
             } yield out

           override def produceTopic(topic: Topic, message: String): ZIO[Connection, Error, Unit] =
             ZIO.environment[Connection].flatMap { conn =>
               getSession(conn).use { sess =>
                 getProducer(sess, topic).use { prod =>
                   blocking.get.effectBlocking {
                     prod.send(sess.createTextMessage(message))
                   }.mapError(ProducerError)
                 }
               }
             }
         }
      }
    }

    def consumeTopic(topic: Topic): ZStream[ActiveMQ with Connection, Error, Message] =
      ZStream.accessStream(_.get.consumeTopic(topic))

    def produceTopic(topic: Topic, message: String): ZIO[ActiveMQ with Connection, Error, Unit] =
      ZIO.accessM(_.get.produceTopic(topic, message))

    val live: ZLayer[Blocking, Nothing, ActiveMQ] = Service.live

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


}
