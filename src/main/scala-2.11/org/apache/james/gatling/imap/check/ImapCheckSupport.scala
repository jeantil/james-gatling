package org.apache.james.gatling.imap.check

import io.gatling.commons.validation.Failure
import io.gatling.core.check.CheckResult

trait ImapCheckSupport {
  def ok = ImapSimpleCheck(_.isOk)

  def hasRecent(expected: Int) = ImapValidationCheck { responses =>
    responses.countRecent match {
      case Some(count) if count == expected => CheckResult.NoopCheckResultSuccess
      case Some(count) => Failure(s"Expected $expected recent messages but got $count")
      case None => Failure(s"Imap protocol violation : no valid RECENT response received")
    }
  }

  def hasNoRecent = hasRecent(0)
}