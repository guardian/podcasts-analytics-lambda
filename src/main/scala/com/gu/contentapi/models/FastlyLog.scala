package com.gu.contentapi.models

import org.apache.logging.log4j.scala.Logging

import scala.util.Try

/**
 * Log format:
 *
 * "" "%t" "req.request" "req.url" "req.http.host" "resp.status" "client.ip" "req.http.user-agent" "req.http.referer" "req.http.Range" "req.header_bytes_read" "req.body_bytes_read" "resp.http.Content-Type" "resp.http.Age" "geoip.country_code" "geoip.region" "geoip.city" "resp.http.Location"
 */

case class FastlyLog(
  time: String,
  request: String,
  url: String, /* relative URL */
  host: String,
  status: String,
  ipAddress: String,
  userAgent: String,
  referrer: String,
  range: String,
  headerBytesRead: String,
  bodyBytesRead: String,
  contentType: String,
  age: String,
  countryCode: String,
  region: String,
  city: String,
  location: String)

object FastlyLog extends Logging {

  def apply(line: String): Option[FastlyLog] = {
    for {
      withoutHeader <- removeFastlyFootprint(line)
      fastlyLog <- parseLine(withoutHeader)
    } yield {
      fastlyLog
    }
  }

  private def parseLine(withoutHeader: String): Option[FastlyLog] = {
    val triedLog = Try {
      val fields = withoutHeader.drop(1).dropRight(1).split("\" \"")
      val cleanedFields = fields.map(replaceNullElements andThen removeEscapedQuotes)
      FastlyLog(
        cleanedFields(0),
        cleanedFields(1),
        cleanedFields(2),
        cleanedFields(3),
        cleanedFields(4),
        cleanedFields(5),
        cleanedFields(6),
        cleanedFields(7),
        cleanedFields(8),
        cleanedFields(9),
        cleanedFields(10),
        cleanedFields(11),
        cleanedFields(12),
        cleanedFields(13),
        cleanedFields(14),
        cleanedFields(15),
        cleanedFields(16)
      )
    }
    triedLog.toEither.left.foreach { e =>
      logger.warn(s"Failed to parse line: '$withoutHeader': ${e.getMessage}", e)
    }
    triedLog.toOption
  }

  private def removeFastlyFootprint(line: String): Option[String] = line.split(""": "" """).tail.headOption

  private def replaceNullElements: String => String = _.replaceAll("""\(null\)""", "")

  private def removeEscapedQuotes: String => String = _.replaceAll("""\\"""", "")

  /* filter out partial content requests unless the byte-range starts from 0 and is not 0-1 */
  private val allowedRangePattern = """^bytes=0-(?!1$)""".r
  val onlyDownloads: FastlyLog => Boolean = log =>
    log.status != "206" || allowedRangePattern.findFirstIn(log.range).nonEmpty

  /* filter out HEAD and any non GET requests */
  val onlyGet: FastlyLog => Boolean = { log => log.request == "GET" }

}

