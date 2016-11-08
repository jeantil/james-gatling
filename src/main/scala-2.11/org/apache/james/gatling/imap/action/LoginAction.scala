package org.apache.james.gatling.imap.action

import scala.collection.immutable.Seq

import akka.actor.{Actor, Props}
import io.gatling.commons.stats.{KO, OK}
import io.gatling.commons.util.TimeHelper._
import io.gatling.commons.validation.{Failure, Validation}
import io.gatling.core.action.ValidatedActionActor
import io.gatling.core.check.Check
import io.gatling.core.session._
import io.gatling.core.stats.message.ResponseTimings
import org.apache.james.gatling.imap.check.ImapCheck
import org.apache.james.gatling.imap.protocol.{Command, Response}

object LoginAction {
  def props(imapContext:ImapActionContext,requestname: String, checks: Seq[ImapCheck], username: Expression[String], password: Expression[String]) =
    Props(new LoginAction(imapContext,requestname, checks, username, password))
}

class LoginAction(val imapContext:ImapActionContext,requestName: String, checks: Seq[ImapCheck], username: Expression[String], password: Expression[String]) extends ValidatedActionActor with ImapActionActor{

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