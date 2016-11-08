package org.apache.james.gatling.imap.action

import akka.actor.Props
import io.gatling.commons.util.TimeHelper._
import io.gatling.core.action.ActionActor
import io.gatling.core.session.Session
import org.apache.james.gatling.imap.protocol.Command

object ConnectAction {
  def props(imapContext: ImapActionContext, requestName: String): Props =
    Props(new ConnectAction(imapContext, requestName))
}

class ConnectAction(val imapContext: ImapActionContext, val requestName: String) extends ActionActor with ImapActionActor {
  override def execute(session: Session): Unit = {
    sessions.tell(Command.Connect(session.userId.toString), handleResponse(session, nowMillis))
  }

}
