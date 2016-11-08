package org.apache.james.gatling.imap.check

import com.sun.mail.imap.protocol.IMAPResponse
import io.gatling.commons.validation.{Failure, Validation}
import io.gatling.core.check.CheckResult

trait ImapCheckSupport {
  def ok = ImapSimpleCheck(_.lastOption.exists(_.isOK))
  val Recent = """^\* (\d+) RECENT$""".r
  def f(count:Int)(responses: Seq[IMAPResponse]): Validation[CheckResult] = {
    responses.find(_.toString.matches(Recent.regex))
        .map(_.toString).map{
          case Recent(actual) if actual==count.toString => CheckResult.NoopCheckResultSuccess
          case Recent(actual) => Failure(s"Expected $count recent messages but got $actual")
        }.getOrElse(Failure(s"Imap protocol violation : no valid RECENT response received"))
  }

  def hasRecent(count: Int) = ImapValidationCheck(f(count))

  def hasNoRecent = hasRecent(0)
}