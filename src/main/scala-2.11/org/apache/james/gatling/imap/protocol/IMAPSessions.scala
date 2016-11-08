package org.apache.james.gatling.imap.protocol

import java.net.URI
import java.util
import javax.xml.ws.ResponseWrapper

import scala.collection.immutable.Seq
import scala.util.control.NoStackTrace

import akka.actor.{ActorRef, Props, Stash}
import com.lafaspot.imapnio.client.{IMAPClient, IMAPSession => ClientSession}
import com.lafaspot.imapnio.listener.IMAPConnectionListener
import com.lafaspot.logfast.logging.internal.LogPage
import com.lafaspot.logfast.logging.{LogManager, Logger}
import com.sun.mail.imap.protocol.IMAPResponse
import io.gatling.core.akka.BaseActor
import org.apache.james.gatling.imap.protocol.command.{LoginHandler, SelectHandler}

object IMAPSessions {
  def props(protocol: ImapProtocol): Props = Props(new IMAPSessions(protocol))
}

class IMAPSessions(protocol: ImapProtocol) extends BaseActor {
  val imapClient = new IMAPClient(4)

  override def receive: Receive = {
    case cmd: Command =>
      sessionFor(cmd.userId).forward(cmd)
  }

  private def sessionFor(userId: String) = {
    context.child(userId).getOrElse(createIMAPSession(userId))
  }

  protected def createIMAPSession(userId: String) = {
    context.actorOf(IMAPSession.props(imapClient, protocol), userId)
  }

  @scala.throws[Exception](classOf[Exception])
  override def postStop(): Unit = {
    super.postStop()
    imapClient.shutdown()
  }
}

private object IMAPSession {
  def props(client: IMAPClient, protocol: ImapProtocol): Props =
    Props(new IMAPSession(client, protocol))

}

private class IMAPSession(client: IMAPClient, protocol: ImapProtocol) extends BaseActor with Stash {
  val connectionListener = new IMAPConnectionListener {
    override def onConnect(session: ClientSession): Unit = {
      logger.trace("Callback onConnect called")
      self ! Response.Connected(Seq.empty[IMAPResponse])
    }

    override def onMessage(session: ClientSession, response: IMAPResponse): Unit =
      logger.trace("Callback onMessage called")


    override def onDisconnect(session: ClientSession, cause: Throwable): Unit = {
      logger.trace("Callback onDisconnect called")
      self ! Response.Disconnected(cause)
    }

    override def onInactivityTimeout(session: ClientSession): Unit =
      logger.trace("Callback onInactivityTimeout called")

    override def onResponse(session: ClientSession, tag: String, responses: util.List[IMAPResponse]): Unit =
      logger.trace("Callback onResponse called")
  }
  val uri = new URI(s"imap://${protocol.host}:${protocol.port}")
  val config = protocol.config
  logger.debug(s"connecting to $uri with $config")
  val session = client.createSession(uri, config, connectionListener, new LogManager(Logger.Level.FATAL, LogPage.DEFAULT_SIZE))
  var tagCounter: Int = 1

  override def receive: Receive = disconnected

  def disconnected: Receive = {
    case Command.Connect(userId) =>
      logger.debug(s"got connect request, $userId connecting to $uri")
      session.connect()
      context.become(connecting(sender()))
    case Response.Disconnected(_) => ()
    case Command.Disconnect(_) => ()
    case msg =>
      logger.error(s"disconnected - unexpected message from ${sender.path} " + msg)
      if(sender.path != self.path)
        sender ! ImapStateError(s"session for ${self.path.name} is not connected")
  }

  def connecting(receiver: ActorRef): Receive = {
    case msg@Response.Connected(responses) =>
      logger.debug("got connected response")
      context.become(connected)
      receiver ! msg
      unstashAll()
    case msg@Response.Disconnected(cause) =>
      context.become(disconnected)
      receiver ! msg
      unstashAll()
    case x =>
      logger.error(s"connecting - got unexpected message $x")
      stash
  }

  def connected: Receive = {
    case cmd@Command.Login(_, _, _) =>
      val tag = f"A$tagCounter%06d"
      val handler = context.actorOf(LoginHandler.props(session, tag), "login")
      handler forward cmd
    case cmd@Command.Select(_, _) =>
      val tag = f"A$tagCounter%06d"
      val handler = context.actorOf(SelectHandler.props(session, tag), "select")
      handler forward cmd
    case msg@Response.Disconnected(cause) =>
      context.become(disconnected)
    case msg@Command.Disconnect(userId)=>
      session.disconnect()
      context.become(disconnecting(sender()))

  }

  def disconnecting(receiver: ActorRef): Receive = {
    case msg@Response.Disconnected(cause) =>
      receiver ! msg
      context.become(disconnected)
      unstashAll()
    case x =>
      logger.error(s"disconnecting - got unexpected message $x")
      stash
  }
}
case class ImapStateError(msg:String)extends IllegalStateException(msg) with NoStackTrace

