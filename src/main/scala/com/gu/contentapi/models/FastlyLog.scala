package com.gu.contentapi.models

import purecsv.safe._
import scala.util.Success

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

  def apply(line: String): Option[FastlyLog] = {

    val maybeLogs = cleanLog(line) flatMap { cleanLine =>
      CSVReader[FastlyLog].readCSVFromString(cleanLine).headOption
    }

    maybeLogs collect { case Success(parsedLog) => parsedLog }
  }

  private def removeFastlyFootprint(line: String): Option[String] = line.split(""" "" """).tail.headOption

  private def makeCsvLike(line: Option[String]): Option[String] = line.map(_.replaceAll("""" """", """",""""))

  private def replaceNullElements(line: Option[String]): Option[String] = line.map(_.replaceAll("""\(null\)""", ""))

  // turns a log line into a nicely formatted csv string we can fit in our model

  val cleanLog: String => Option[String] = removeFastlyFootprint _ andThen makeCsvLike andThen replaceNullElements

  /* filter out partial content requests */
  val onlyDownloads: FastlyLog => Boolean = { log => log.status != "206" }

}

