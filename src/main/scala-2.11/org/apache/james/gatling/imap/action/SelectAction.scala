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
import org.apache.james.gatling.imap.protocol.{Command, ImapResponses, Response}

object SelectAction {
  def props(imapContext: ImapActionContext, requestname: String, checks: Seq[ImapCheck], mailbox: Expression[String]) =
    Props(new SelectAction(imapContext, requestname, checks, mailbox))
}

class SelectAction(val imapContext: ImapActionContext, requestName: String, checks: Seq[ImapCheck], mailbox: Expression[String]) extends ValidatedActionActor with ImapActionActor {
  def handleSelected(session: Session, start: Long) =
    context.actorOf(Props(new Actor {
      override def receive: Receive = {
        case Response.Selected(response: ImapResponses) =>
          logger.trace("SelectAction#Selected on SelectHandler.Selected")
          val (checkSaveUpdate, error) = Check.check(response, session, checks.toList)
          error.fold(ok(session, start, checkSaveUpdate))(ko(session, start))
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

      def ok(session: Session, start: Long, updateSession: Session => Session) = {
        statsEngine.logResponse(session, requestName, ResponseTimings(start, nowMillis), OK, None, None)
        next ! updateSession(session)
        context.stop(self)
      }
    }), s"user-${session.userId}")

  override protected def executeOrFail(session: Session): Validation[_] = {
    for {
      mailbox <- mailbox(session)
    } yield {
      val id: Long = session.userId
      val handler = handleSelected(session, nowMillis)
      sessions.tell(Command.Select(id.toString, mailbox), handler)
    }
  }
}