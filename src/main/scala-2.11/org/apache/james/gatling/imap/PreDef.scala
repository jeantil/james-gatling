package org.apache.james.gatling.imap

import org.apache.james.gatling.imap.check.ImapCheckSupport
import org.apache.james.gatling.imap.protocol.ImapProtocol

object PreDef extends ImapCheckSupport {

  def imap = ImapProtocolBuilder.default
  implicit def imapProtocolBuilder2ImapProtocol(builder: ImapProtocolBuilder): ImapProtocol = builder.build()

  def imap(requestName: String) = new ImapActionBuilder(requestName)
}
