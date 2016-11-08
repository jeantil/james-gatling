package org.apache.james.gatling.imap.check

import scala.collection.mutable

import io.gatling.commons.validation.Validation
import io.gatling.core.Predef.Session
import io.gatling.core.check.CheckResult
import org.apache.james.gatling.imap.protocol.ImapResponses

case class ImapValidationCheck(func: ImapResponses => Validation[CheckResult], message:String=ImapSimpleCheck.DefaultMessage) extends ImapCheck {
  override def check(responses: ImapResponses, session: Session)(implicit cache: mutable.Map[Any, Any]): Validation[CheckResult] = {
    func(responses)
  }
}
