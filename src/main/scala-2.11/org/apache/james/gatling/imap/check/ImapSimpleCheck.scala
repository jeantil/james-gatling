package org.apache.james.gatling.imap.check

import scala.collection.mutable

import io.gatling.commons.validation.{Failure, Validation}
import io.gatling.core.check.CheckResult
import io.gatling.core.session.Session
import org.apache.james.gatling.imap.protocol.ImapResponses

object ImapSimpleCheck{
  val DefaultMessage = "Imap check failed"
}
case class ImapSimpleCheck(func: ImapResponses => Boolean, message:String=ImapSimpleCheck.DefaultMessage) extends ImapCheck {
  override def check(responses: ImapResponses, session: Session)(implicit cache: mutable.Map[Any, Any]): Validation[CheckResult] = {
    func(responses) match {
      case true => CheckResult.NoopCheckResultSuccess
      case _    => Failure(message)
    }
  }
}
