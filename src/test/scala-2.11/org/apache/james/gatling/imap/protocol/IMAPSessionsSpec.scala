package org.apache.james.gatling.imap.protocol

import java.util.Properties

import scala.collection.immutable.Seq
import scala.concurrent.duration._

import akka.actor.ActorSystem
import akka.testkit.TestProbe
import com.sun.mail.imap.protocol.IMAPResponse
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpec}
import org.slf4j.LoggerFactory

class IMAPSessionsSpec extends WordSpec with Matchers with BeforeAndAfterAll{
  private val logger = LoggerFactory.getLogger(this.getClass.getCanonicalName)
  implicit lazy val system = ActorSystem("LoginHandlerSpec")
  "the imap sessions actor" should {
    "log a user in" in {
      val config=new Properties()
      val protocol = ImapProtocol("10.69.0.110", config=config)

      val sessions = system.actorOf(IMAPSessions.props(protocol))
      val probe = TestProbe()
      probe.send(sessions, Command.Connect("1"))
      probe.expectMsg(10.second,Response.Connected(Seq.empty[IMAPResponse]))
      probe.send(sessions, Command.Login("1", "user1", "password"))
      probe.expectMsgPF(10.second) {
        case s: Seq[IMAPResponse] => s.exists(_.isOK) shouldBe true
      }
    }
  }

  override protected def afterAll(): Unit = {
    system.terminate()
  }
}
