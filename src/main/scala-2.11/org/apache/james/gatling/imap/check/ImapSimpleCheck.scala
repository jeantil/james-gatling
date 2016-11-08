package org.apache.james.gatling.imap.check

import scala.collection.immutable.Seq
import scala.collection.mutable

import com.sun.mail.imap.protocol.IMAPResponse
import io.gatling.commons.validation.{Failure, Validation}
import io.gatling.core.check.CheckResult
import io.gatling.core.session.Session

case class ImapSimpleCheck(func: Seq[IMAPResponse] => Boolean) extends ImapCheck {
  override def check(responses: Seq[IMAPResponse], session: Session)(implicit cache: mutable.Map[Any, Any]): Validation[CheckResult] = {
    func(responses) match {
      case true => CheckResult.NoopCheckResultSuccess
      case _    => Failure("Imap check failed")
    }
  }
}
