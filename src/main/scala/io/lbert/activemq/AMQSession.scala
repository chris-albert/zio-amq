package io.lbert.activemq

object AMQSession {

  sealed trait AcknowledgeMode {
    val modeInt: Int
  }

  object AcknowledgeMode {
    final case object AutoAcknowledge extends AcknowledgeMode {
      override val modeInt: Int = javax.jms.Session.AUTO_ACKNOWLEDGE
    }
    final case object ClientAcknowledge extends AcknowledgeMode {
      override val modeInt: Int = javax.jms.Session.CLIENT_ACKNOWLEDGE
    }
    final case object DupsOkAcknowledge extends AcknowledgeMode {
      override val modeInt: Int = javax.jms.Session.DUPS_OK_ACKNOWLEDGE
    }
    final case object SessionTransacted extends AcknowledgeMode {
      override val modeInt: Int = javax.jms.Session.SESSION_TRANSACTED
    }
  }

  final case class Config(transacted: Boolean, acknowledgeMode: AcknowledgeMode)

  object Config {
    val default: Config = Config(transacted = false, AcknowledgeMode.AutoAcknowledge)
  }
}
