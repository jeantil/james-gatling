package org.apache.james.gatling.imap.action

import scala.collection.immutable.Seq

import akka.actor.{Actor, ActorRef}
import io.gatling.core.action.Action
import io.gatling.core.check.Check
import io.gatling.core.session.Session
import io.gatling.core.stats.StatsEngine
import org.apache.james.gatling.imap.protocol.ImapResponses

trait ImapActionActor {
  self: Actor =>

  def imapContext: ImapActionContext

  def requestName: String

  def statsEngine: StatsEngine = imapContext.statsEngine

  def next: Action = imapContext.next

  def sessions: ActorRef = imapContext.sessions

  def checks: Seq[Check[ImapResponses]]= Seq.empty

  protected def handleResponse(session: Session, start: Long): ActorRef =
    context.actorOf(ImapResponseHandler.props(imapContext, requestName, session, start, checks), s"user-${session.userId}")
}
