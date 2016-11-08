package org.apache.james.gatling.imap.action

import akka.actor.{Actor, Props}
import io.gatling.commons.stats.{KO, OK}
import io.gatling.commons.util.TimeHelper._
import io.gatling.core.action.ActionActor
import io.gatling.core.session.Session
import io.gatling.core.stats.message.ResponseTimings
import org.apache.james.gatling.imap.protocol.{Command, Response}

object ConnectAction {
  def props(imapContext:ImapActionContext, requestName: String): Props =
    Props(new ConnectAction(imapContext,requestName))
}

class ConnectAction(val imapContext:ImapActionContext, requestName: String) extends ActionActor with ImapActionActor{
  override def execute(session: Session): Unit = {
    sessions.tell(Command.Connect(session.userId.toString), handleConnected(session, nowMillis))
  }

  def handleConnected(session: Session, start: Long) =
    context.actorOf(Props(new Actor {
      override def receive: Receive = {
        case Response.Connected(responses) =>
          ok()
        case Response.Disconnected(cause) =>
          logger.warn("Connection failure", cause)
          ko(Some(cause.getMessage))
        case e: Exception =>
          ko(Some(e.getMessage))
      }

      def ko(message: Option[String] = None) = {
        statsEngine.logResponse(session, requestName, ResponseTimings(start, nowMillis), KO, None, message)
        next ! session.markAsFailed
        context.stop(self)
      }

      def ok() = {
        statsEngine.logResponse(session, requestName, ResponseTimings(start, nowMillis), OK, None, None)
        next ! session
        context.stop(self)
      }
    }))
}
