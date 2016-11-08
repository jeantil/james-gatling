package org.apache.james.gatling.imap.action

import scala.collection.immutable.Seq

import akka.actor.{Actor, ActorRef, Props}
import io.gatling.commons.stats.{KO, OK}
import io.gatling.commons.util.TimeHelper._
import io.gatling.commons.validation.{Failure, Validation}
import io.gatling.core.action.{Action, ValidatedActionActor}
import io.gatling.core.check.Check
import io.gatling.core.session._
import io.gatling.core.stats.StatsEngine
import io.gatling.core.stats.message.ResponseTimings
import org.apache.james.gatling.imap.check.ImapCheck
import org.apache.james.gatling.imap.protocol.{Command, ImapProtocol, Response}

object LoginAction {
  def props(protocol: ImapProtocol, sessions: ActorRef, requestname: String, statsEngine: StatsEngine, next: Action,
            checks: Seq[ImapCheck], username: Expression[String], password: Expression[String]) =
    Props(new LoginAction(protocol, sessions, requestname, statsEngine, next, checks, username, password))
}

class LoginAction(protocol: ImapProtocol, sessions: ActorRef, requestName: String, val statsEngine: StatsEngine, val next: Action, checks: Seq[ImapCheck], username: Expression[String], password: Expression[String]) extends ValidatedActionActor {

  def handleLoggedIn(session: Session, start: Long) =
    context.actorOf(Props(new Actor {
      override def receive: Receive = {
        case Response.LoggedIn(response) =>
          logger.trace("LoginAction#handleLoggedIn on LoginHandler.LoggedIn")
          val (checkSaveUpdate, error) = Check.check(response,session, checks.toList)
          error.fold(Ok(session, start,checkSaveUpdate))(Ko(session, start))

        case e: Exception =>
          Ko(session, start)(Failure(e.getMessage))
        case msg =>
          logger.error(s"received unexpected message $msg")

      }

      def Ko(session: Session, start: Long)(failure: Failure) = {
        statsEngine.logResponse(session, requestName, ResponseTimings(start, nowMillis), KO, None, Some(failure.message))
        next ! session.markAsFailed
        context.stop(self)
      }

      def Ok(session: Session, start: Long, updateSession: Session => Session) = {
        statsEngine.logResponse(session, requestName, ResponseTimings(start, nowMillis), OK, None, None)
        next ! updateSession(session)
        context.stop(self)
      }
    }), s"user-${session.userId}")

  override protected def executeOrFail(session: Session): Validation[_] = {
    for {
      user <- username(session)
      pass <- password(session)
    } yield {
      val id: Long = session.userId
      val handler = handleLoggedIn(session, nowMillis)
      sessions.tell(Command.Login(id.toString, user, pass), handler)
    }
  }
}