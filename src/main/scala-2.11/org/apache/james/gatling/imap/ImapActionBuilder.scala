package org.apache.james.gatling.imap

import io.gatling.core.action.builder.ActionBuilder
import io.gatling.core.action.{Action, ExitableActorDelegatingAction}
import io.gatling.core.session.Expression
import io.gatling.core.structure.ScenarioContext
import io.gatling.core.util.NameGen
import org.apache.james.gatling.imap.action.{ConnectAction, LoginAction, SelectAction}
import org.apache.james.gatling.imap.check.ImapCheck
import org.apache.james.gatling.imap.protocol.{ImapComponents, ImapProtocol}
import scala.collection.immutable.Seq

class ImapActionBuilder(requestName: String) {
  def login(user:Expression[String], password:Expression[String]): ImapLoginActionBuilder = {
    ImapLoginActionBuilder(requestName, user,password)
  }
  def select(mailbox:Expression[String]): ImapSelectActionBuilder = {
    ImapSelectActionBuilder(requestName, mailbox, Seq.empty)
  }
  def connect(): ImapConnectActionBuilder = {
    ImapConnectActionBuilder(requestName)
  }
}

case class ImapLoginActionBuilder(requestName: String, username:Expression[String], password:Expression[String]) extends ActionBuilder with NameGen {
  override def build(ctx: ScenarioContext, next: Action): Action = {
    val components: ImapComponents = ctx.protocolComponentsRegistry.components(ImapProtocol.ImapProtocolKey)
    val actionActor = ctx.system.actorOf(LoginAction.props(components.protocol, components.sessions, requestName, ctx.coreComponents.statsEngine, next, username, password), "login-action")
    new ExitableActorDelegatingAction(genName(requestName), ctx.coreComponents.statsEngine, next, actionActor)
  }
}

case class ImapSelectActionBuilder(requestName: String, mailbox:Expression[String], private val checks: Seq[ImapCheck]) extends ActionBuilder with NameGen {
  def check(checks: ImapCheck*) = copy(checks=this.checks ++ checks)
  override def build(ctx: ScenarioContext, next: Action): Action = {
    val components: ImapComponents = ctx.protocolComponentsRegistry.components(ImapProtocol.ImapProtocolKey)
    val actionActor = ctx.system.actorOf(SelectAction.props(components.protocol, components.sessions, requestName, ctx.coreComponents.statsEngine, next, checks, mailbox), "select-action")
    new ExitableActorDelegatingAction(genName(requestName), ctx.coreComponents.statsEngine, next, actionActor)
  }
}

case class ImapConnectActionBuilder(requestName: String) extends ActionBuilder with NameGen {
  override def build(ctx: ScenarioContext, next: Action): Action = {
    val components: ImapComponents = ctx.protocolComponentsRegistry.components(ImapProtocol.ImapProtocolKey)
    val actionActor = ctx.system.actorOf(ConnectAction.props(components.protocol, components.sessions, requestName, ctx.coreComponents.statsEngine, next),"connect-action")
    new ExitableActorDelegatingAction(genName(requestName), ctx.coreComponents.statsEngine, next, actionActor)
  }
}