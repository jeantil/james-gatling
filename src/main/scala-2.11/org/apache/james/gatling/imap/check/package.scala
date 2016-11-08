package org.apache.james.gatling.imap

import scala.collection.immutable.Seq

import com.sun.mail.imap.protocol.IMAPResponse
import io.gatling.core.check.Check

package object check {
  type ImapCheck = Check[Seq[IMAPResponse]]
}