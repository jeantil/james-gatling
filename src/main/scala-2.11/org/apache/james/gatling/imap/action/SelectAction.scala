package org.apache.james.gatling.imap.action

import scala.collection.immutable.Seq

import akka.actor.{Actor, ActorRef, Props}
import com.sun.mail.imap.protocol.IMAPResponse
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

object SelectAction {
  def props(protocol: ImapProtocol, sessions: ActorRef, requestname: String, statsEngine: StatsEngine, next: Action, checks: Seq[ImapCheck], mailbox:Expression[String]) =
    Props(new SelectAction(protocol, sessions, requestname,statsEngine, next, checks, mailbox))
}

class SelectAction(protocol: ImapProtocol, sessions: ActorRef, requestName: String, val statsEngine: StatsEngine, val next: Action, checks: Seq[ImapCheck], mailbox: Expression[String]) extends ValidatedActionActor {

  def handleSelected(session: Session, start: Long) =
    context.actorOf(Props( new Actor {
    override def receive: Receive = {
      case Response.Selected(response: Seq[IMAPResponse]) =>
        logger.trace("SelectAction#Selected on SelectHandler.Selected")
        val (checkSaveUpdate, error) = Check.check(response,session, checks.toList)
        error.fold(ok(session, start,checkSaveUpdate))(ko(session, start))
      case e: Exception =>
        ko(session, start)(Failure(e.getMessage))
      case msg =>
        logger.error(s"received unexpected message $msg")

    }

    def ko(session: Session, start: Long)(failure: Failure) = {
      statsEngine.logResponse(session, requestName, ResponseTimings(start, nowMillis), KO, None, Some(failure.message))
      next ! session.markAsFailed
      context.stop(self)
    }

    def ok(session: Session, start: Long, updateSession: Session=>Session) = {
      statsEngine.logResponse(session, requestName, ResponseTimings(start, nowMillis), OK, None, None)
      next ! updateSession(session)
      context.stop(self)
    }
  }), s"user-${session.userId}")

  override protected def executeOrFail(session: Session): Validation[_] = {
      for{
        mailbox<-mailbox(session)
      } yield {
        val id: Long = session.userId
        val handler =handleSelected(session, nowMillis)
        sessions.tell(Command.Select(id.toString, mailbox),handler)
      }
  }
}