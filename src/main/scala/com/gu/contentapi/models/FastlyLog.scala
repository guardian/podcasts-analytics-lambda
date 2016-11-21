package com.gu.contentapi.models

case class FastlyLog(
  time: String,
  request: String,
  url: String,
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
  location: String
)

object FastlyLog {

  // turns a log line into a nicely formatted csv string we can fit in our model

  private def removeFastlyFootprint(line: String): Option[String] = line.split(""" "" """).tail.headOption

  private def makeCsvLike(line: Option[String]): Option[String] = line.map(_.replaceAll("""" """", """",""""))

  private def replaceNullElements(line: Option[String]): Option[String] = line.map(_.replaceAll("""\(null\)""", ""))

  val cleanLog: String => Option[String] = removeFastlyFootprint _ andThen makeCsvLike andThen replaceNullElements

}

