package org.apache.james.gatling.imap.action

import io.gatling.core.action.Action
import io.gatling.core.stats.StatsEngine

trait ImapActionActor {
  def imapContext: ImapActionContext

  def statsEngine: StatsEngine = imapContext.statsEngine

  def next: Action = imapContext.next
  def sessions = imapContext.sessions
}
