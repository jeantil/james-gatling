package org.apache.james.gatling.imap.check

import scala.collection.immutable.Seq
import scala.collection.mutable


import com.sun.mail.imap.protocol.IMAPResponse
import io.gatling.commons.validation.Validation
import io.gatling.core.Predef.Session
import io.gatling.core.check.CheckResult

case class ImapValidationCheck(func: Seq[IMAPResponse] => Validation[CheckResult], message:String=ImapSimpleCheck.DefaultMessage) extends ImapCheck {
  override def check(responses: Seq[IMAPResponse], session: Session)(implicit cache: mutable.Map[Any, Any]): Validation[CheckResult] = {
    func(responses)
  }
}
