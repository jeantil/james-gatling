package org.apache.james.gatling.imap

import scala.collection.immutable.Seq

import com.sun.mail.imap.protocol.IMAPResponse

package object protocol {

  trait Command {
    def userId: String
  }


  object Command {

    case class Connect(userId: String) extends Command

    case class Login(userId: String, username: String, password: String) extends Command

    case class Select(userId: String, mailbox: String) extends Command

    case class Disconnect(userId: String) extends Command

  }

  object Response {

    case class Connected(responses: ImapResponses)

    case class LoggedIn(response: ImapResponses)

    case class Selected(response: ImapResponses)

    case class Disconnected(cause: Throwable)

  }

}

