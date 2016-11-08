package org.apache.james.gatling.imap.action

import akka.actor.{Actor, ActorRef, Props}
import com.sun.mail.imap.protocol.IMAPResponse
import io.gatling.commons.stats.{KO, OK}
import io.gatling.commons.util.TimeHelper._
import io.gatling.commons.validation.Validation
import io.gatling.core.action.{Action, ValidatedActionActor}
import io.gatling.core.session._
import io.gatling.core.stats.StatsEngine
import io.gatling.core.stats.message.ResponseTimings
import org.apache.james.gatling.imap.protocol.{Command, ImapProtocol, Response}
import org.apache.james.gatling.imap.protocol.command.LoginHandler

object LoginAction {
  def props(protocol: ImapProtocol, sessions: ActorRef, requestname: String, statsEngine: StatsEngine, next: Action, username:Expression[String], password:Expression[String]) =
    Props(new LoginAction(protocol, sessions, requestname,statsEngine, next,username, password))
}

class LoginAction(protocol: ImapProtocol, sessions: ActorRef, requestName: String, val statsEngine: StatsEngine, val next: Action, username:Expression[String], password:Expression[String]) extends ValidatedActionActor {

  def handleLoggedIn(session: Session, start: Long) =
    context.actorOf(Props( new Actor {
    override def receive: Receive = {
      case Response.LoggedIn(response: Seq[IMAPResponse]) =>
        logger.trace("LoginAction#handleLoggedIn on LoginHandler.LoggedIn")
        response.find(_.isOK).fold(noOkResponse(session, start))(onOkResponse(session, start))
      case e: Exception =>
        noOkResponse(session, start, Some(e.getMessage))
      case msg =>
        logger.error(s"received unexpected message $msg")

    }

    def noOkResponse(session: Session, start: Long, message: Option[String] = None) = {
      statsEngine.logResponse(session, requestName, ResponseTimings(start, nowMillis), KO, None, message)
      next ! session.markAsFailed
      context.stop(self)
    }

    def onOkResponse(session: Session, start: Long)(response: IMAPResponse) = {
      statsEngine.logResponse(session, requestName, ResponseTimings(start, nowMillis), OK, None, None)
      next ! session
      context.stop(self)
    }
  }), s"user-${session.userId}")

  override protected def executeOrFail(session: Session): Validation[_] = {
      for{
        user<-username(session)
        pass<-password(session)
      } yield {
        val id: Long = session.userId
        val handler =handleLoggedIn(session, nowMillis)
        sessions.tell(Command.Login(id.toString, user, pass),handler)
      }
  }
}