package org.apache.james.gatling.imap

import io.gatling.core.check.Check
import org.apache.james.gatling.imap.protocol.ImapResponses

package object check {
  type ImapCheck = Check[ImapResponses]
}