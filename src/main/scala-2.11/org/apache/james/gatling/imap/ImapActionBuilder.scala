package org.apache.james.gatling.imap

import scala.collection.immutable.Seq

import akka.actor.Props
import io.gatling.core.action.builder.ActionBuilder
import io.gatling.core.action.{Action, ExitableActorDelegatingAction}
import io.gatling.core.session.Expression
import io.gatling.core.stats.StatsEngine
import io.gatling.core.structure.ScenarioContext
import io.gatling.core.util.NameGen
import org.apache.james.gatling.imap.action.{ConnectAction, LoginAction, SelectAction}
import org.apache.james.gatling.imap.check.ImapCheck
import org.apache.james.gatling.imap.protocol.{ImapComponents, ImapProtocol}

class ImapActionBuilder(requestName: String) {
  def login(user: Expression[String], password: Expression[String]): ImapLoginActionBuilder = {
    ImapLoginActionBuilder(requestName, user, password, Seq.empty)
  }

  def select(mailbox: Expression[String]): ImapSelectActionBuilder = {
    ImapSelectActionBuilder(requestName, mailbox, Seq.empty)
  }

  def connect(): ImapConnectActionBuilder = {
    ImapConnectActionBuilder(requestName)
  }
}

abstract class ImapCommandActionBuilder extends ActionBuilder with NameGen {
  def props(statsEngine: StatsEngine, next: Action, components: ImapComponents): Props

  def requestName: String

  def actionName: String

  override def build(ctx: ScenarioContext, next: Action): Action = {
    val components: ImapComponents = ctx.protocolComponentsRegistry.components(ImapProtocol.ImapProtocolKey)
    val actionActor = ctx.system.actorOf(props(ctx.coreComponents.statsEngine, next, components), actionName)
    new ExitableActorDelegatingAction(genName(requestName), ctx.coreComponents.statsEngine, next, actionActor)
  }

}

case class ImapLoginActionBuilder(requestName: String, username: Expression[String], password: Expression[String], private val checks: Seq[ImapCheck]) extends ImapCommandActionBuilder {
  def check(checks: ImapCheck*) = copy(checks = this.checks ++ checks)

  override def props(statsEngine: StatsEngine, next: Action, components: ImapComponents) =
    LoginAction.props(components.protocol, components.sessions, requestName, statsEngine, next, checks, username, password)

  override val actionName = "login-action"
}

case class ImapSelectActionBuilder(requestName: String, mailbox: Expression[String], private val checks: Seq[ImapCheck]) extends ImapCommandActionBuilder {
  def check(checks: ImapCheck*) = copy(checks = this.checks ++ checks)

  override def props(statsEngine: StatsEngine, next: Action, components: ImapComponents) =
    SelectAction.props(components.protocol, components.sessions, requestName, statsEngine, next, checks, mailbox)

  override val actionName = "select-action"
}

case class ImapConnectActionBuilder(requestName: String) extends ImapCommandActionBuilder {
  override def props(statsEngine: StatsEngine, next: Action, components: ImapComponents): Props =
    ConnectAction.props(components.protocol, components.sessions, requestName, statsEngine, next)

  override val actionName: String = "connect-action"
}