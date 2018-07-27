package com.gu.contentapi.models

import com.gu.contentapi.utils.CsvParser

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

object FastlyLog {

  private val parser = new CsvParser[FastlyLog]

  def apply(line: String): Option[FastlyLog] = cleanLog(line) flatMap parser.parseLine

  private def removeFastlyFootprint(line: String): Option[String] = line.split(""": "" """).tail.headOption

  private def makeCsvLike: String => String = _.replaceAll("""" """", """","""")

  private def replaceNullElements: String => String = _.replaceAll("""\(null\)""", "")

  private def removeEscapedQuotes: String => String = _.replaceAll("""\\"""", "")

  // turns a log line into a nicely formatted csv string we can fit in our model

  val cleanLog: String => Option[String] = removeFastlyFootprint(_) map (makeCsvLike andThen replaceNullElements andThen removeEscapedQuotes)

  /* filter out partial content requests unless the byte-range starts from 0 and is not 0-1 */
  val allowedRangePattern = """^bytes=0-(?!1$)""".r
  val onlyDownloads: FastlyLog => Boolean = log =>
    log.status != "206" || allowedRangePattern.findFirstIn(log.range).nonEmpty

  /* filter out HEAD and any non GET requests */
  val onlyGet: FastlyLog => Boolean = { log => log.request == "GET" }

}

