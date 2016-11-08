package org.apache.james.gatling.imap

import scala.collection.immutable.Seq

import com.sun.mail.imap.protocol.IMAPResponse

package object protocol {
  trait Command {
    def userId: String
  }
  object Command {
    case class Login(userId: String, username: String, password: String) extends Command
    case class Select(userId: String, mailbox: String) extends Command
  }
  object Response {
    case class LoggedIn(response: Seq[IMAPResponse])
    case class Selected(response: Seq[IMAPResponse])
  }
}

