package com.gu.contentapi

import com.gu.contentapi.models.{ Event, AcastLog, FastlyLog }
import org.scalatest.{ FlatSpec, Matchers, OptionValues }

import scala.io.Source

class EventSpec extends FlatSpec with Matchers with OptionValues {

  val fastlyLog1 = FastlyLog(
    time = "Fri, 11 Nov 2016 13:00:00 GMT",
    request = "GET",
    url = "/2016/11/10-58860-FW-10nov-2016_mixdown.mp3",
    host = "composer-uploads-audio.s3-website-eu-west-1.amazonaws.com",
    status = "206",
    ipAddress = "66.87.114.159",
    userAgent = "Samsung SM-G900P stagefright/Beyonce/1.1.9 (Linux;Android 6.0.1)",
    referrer = "",
    range = "bytes=6422528-",
    headerBytesRead = "232",
    bodyBytesRead = "0",
    contentType = "audio/mp3",
    age = "1592",
    countryCode = "US",
    region = "MI",
    city = "Ypsilanti",
    location = ""
  )

  it should "Convert a fastly log to an event ready to be sent to Ophan" in {

    Event(fastlyLog1) should be(Some(Event(
      viewId = "-1969270942156082233",
      url = "https://audio.guim.co.uk/2016/11/10-58860-FW-10nov-2016_mixdown.mp3",
      ipAddress = "66.87.114.159",
      episodeId = "https://www.theguardian.com/football/audio/2016/nov/10/gordon-strachans-last-stand-with-scotland-football-weekly-extra",
      podcastId = "https://www.theguardian.com/football/series/footballweekly",
      ua = "Samsung SM-G900P stagefright/Beyonce/1.1.9 (Linux;Android 6.0.1)"
    )))
  }

  it should "Convert a fastly log and handling platform parameter correctly" in {

    val fastlyLog2 = fastlyLog1.copy(url = fastlyLog1.url + "?platform=amazon-echo")

    Event(fastlyLog2) should be(Some(Event(
      viewId = "-1969270942156082233",
      url = "https://audio.guim.co.uk/2016/11/10-58860-FW-10nov-2016_mixdown.mp3",
      ipAddress = "66.87.114.159",
      episodeId = "https://www.theguardian.com/football/audio/2016/nov/10/gordon-strachans-last-stand-with-scotland-football-weekly-extra",
      podcastId = "https://www.theguardian.com/football/series/footballweekly",
      ua = "Samsung SM-G900P stagefright/Beyonce/1.1.9 (Linux;Android 6.0.1)",
      platform = Some("amazon-echo")
    )))
  }

  it should "When converting a FastlyLog to an Event, it should filter out all the 206 partial content statuses" in {

    val logs: Seq[FastlyLog] = Source.fromFile("src/test/resources/logs-sample.txt")("ISO-8859-1").getLines().toSeq flatMap { line => FastlyLog(line) }

    val logsGrouped = logs.groupBy(f => f.status).mapValues(_.length)

    logsGrouped.get("206").value should be(71)
    logsGrouped.get("200").value should be(12)
    logsGrouped.get("304").value should be(3)

    val events = logs.filter(FastlyLog.onlyDownloads).flatMap(Event(_))

    events.length should be(15)

  }

  val acastLog1 = AcastLog(
    timestamp = "2017-05-18T07:18:00.000Z",
    bytes = "26940335",
    range = "-",
    ipAddress = "87.7.235.91",
    request = "GET",
    url = "/encoded/dcd5264b-a4bc-4fc6-a7d7-23500e04fffd/9f9a7fd1-6f8c-41dc-bb9c-bac2b7d5b31c/encoded-0a2236c4665171be111f0bd8254e29e7-128.mp3",
    status = "200",
    referrer = "-",
    userAgent = "Mozilla/5.0 (Linux; U; en-us; BeyondPod 4)",
    query = "ci=51a96e67-5340-498e-86bf-4509b466dfe5&aid=9f9a7fd1-6f8c-41dc-bb9c-bac2b7d5b31c&chid=dcd5264b-a4bc-4fc6-a7d7-23500e04fffd&st=rpBS51vlxA2p5F5pboQS0Q&e=1495107120&filename=The-Guardians-Science-Weekly-Science-weekly-can-we-cure-Alzheimers-podcast.mp3",
    host = "mediacdn.acast.com",
    forwardedFor = "-",
    cloudfrontResultType = "Hit",
    cloudfrontResponseResultType = "Hit",
    cloudfrontRequestId = "5AC0OQv86ccuUcBhYhaMw1-WRPri8BaptTAAermlanefKXVN-YyJoQ==",
    city = "Rome",
    country = "Italy",
    countryCode = "IT",
    region = "RM",
    continentCode = "EU",
    dmaCode = "0",
    postalCode = "00126",
    longitude = "12.4833",
    latitude = "41.9"
  )

  it should "Convert a acast log to an event ready to be sent to Ophan" in {

    Event(acastLog1) should be(Some(Event(
      viewId = "5AC0OQv86ccuUcBhYhaMw1-WRPri8BaptTAAermlanefKXVN-YyJoQ==",
      url = "https://audio.guim.co.uk/2017/05/12-48386-gnl.rs.20170521.scienceweekly.alzheimers.mp3",
      ipAddress = "87.7.235.91",
      episodeId = "https://www.theguardian.com/science/audio/2017/may/14/science-weekly-can-we-cure-alzheimers-podcast",
      podcastId = "https://www.theguardian.com/science/series/science",
      ua = "Mozilla/5.0 (Linux; U; en-us; BeyondPod 4)"
    )))
  }

}