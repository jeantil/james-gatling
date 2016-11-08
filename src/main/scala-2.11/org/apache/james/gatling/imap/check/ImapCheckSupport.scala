package org.apache.james.gatling.imap.check

trait ImapCheckSupport {
  def ok = ImapSimpleCheck(_.lastOption.exists(_.isOK))
}
