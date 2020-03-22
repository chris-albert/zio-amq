package io.lbert.activemq

final case class AMQConnection(
  url: AMQConnection.AMQUrl,
  credentials: Option[AMQConnection.AMQCredentials]
)

object AMQConnection {

  final case class AMQUrl(url: String)

  final case class AMQCredentials(
    user: Username,
    pass: Password
  )

  final case class Username(user: String)

  final case class Password(pass: String)
}