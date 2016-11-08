package org.apache.james.gatling.imap.protocol

import scala.collection.immutable.Seq

import com.sun.mail.imap.protocol.IMAPResponse

case class ImapResponses(responses: Seq[IMAPResponse]) {
  import ImapResponses._

  def mkString(separator: String = ",") = {
    responses.mkString(separator)
  }

  def isBad = responses.lastOption.exists(_.isBAD)

  def isOk = responses.lastOption.exists(_.isOK)

  def countRecent: Option[Int] = {
    responses.map(_.toString).find(_.matches(Recent.regex))
      .map {
        case Recent(actual) => actual.toInt
      }
  }
}

object ImapResponses {
  val empty = ImapResponses(Seq.empty)
  private val Recent = """^\* (\d+) RECENT$""".r
}
