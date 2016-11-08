package org.apache.james.gatling.imap.action

import akka.actor.{Actor, ActorRef, Props}
import io.gatling.commons.stats.{KO, OK}
import io.gatling.commons.util.TimeHelper._
import io.gatling.core.action.{Action, ActionActor}
import io.gatling.core.session.Session
import io.gatling.core.stats.StatsEngine
import io.gatling.core.stats.message.ResponseTimings
import org.apache.james.gatling.imap.protocol.{Command, ImapProtocol, Response}

object ConnectAction {
  def props(protocol: ImapProtocol, sessions: ActorRef, requestName: String, statsEngine: StatsEngine, next: Action): Props =
    Props(new ConnectAction(protocol, sessions, requestName, statsEngine, next))
}

class ConnectAction(protocol: ImapProtocol, sessions: ActorRef, requestName: String, val statsEngine: StatsEngine, val next: Action) extends ActionActor {
  override def execute(session: Session): Unit = {
    sessions.tell(Command.Connect(session.userId.toString), handleConnected(session, nowMillis))
  }

  def handleConnected(session: Session, start: Long) =
    context.actorOf(Props(new Actor {
      override def receive: Receive = {
        case Response.Connected(responses) =>
          onOkResponse()
        case Response.Disconnected(cause) =>
          logger.warn("Connection failure", cause)
          noOkResponse(Some(cause.getMessage))
        case e: Exception =>
          noOkResponse(Some(e.getMessage))
      }

      def noOkResponse(message: Option[String] = None) = {
        statsEngine.logResponse(session, requestName, ResponseTimings(start, nowMillis), KO, None, message)
        next ! session.markAsFailed
        context.stop(self)
      }

      def onOkResponse() = {
        statsEngine.logResponse(session, requestName, ResponseTimings(start, nowMillis), OK, None, None)
        next ! session
        context.stop(self)
      }
    }))
}
