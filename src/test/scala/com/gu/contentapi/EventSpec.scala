package com.gu.contentapi

import com.gu.contentapi.models.{ Event, FastlyLog }
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.OptionValues
import org.scalatest.matchers.should._
import scala.io.Source

class EventSpec extends AnyFlatSpec with Matchers with OptionValues {

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
    location = "")

  it should "Convert a fastly log to an event ready to be sent to Ophan" in {

    Event(fastlyLog1) should be(Some(Event(
      viewId = "-1969270942156082233",
      url = "https://audio.guim.co.uk/2016/11/10-58860-FW-10nov-2016_mixdown.mp3",
      ipAddress = "66.87.114.159",
      episodeId = "https://www.theguardian.com/football/audio/2016/nov/10/gordon-strachans-last-stand-with-scotland-football-weekly-extra",
      podcastId = "https://www.theguardian.com/football/series/footballweekly",
      ua = "Samsung SM-G900P stagefright/Beyonce/1.1.9 (Linux;Android 6.0.1)")))
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
      platform = Some("amazon-echo"))))
  }

  it should "Convert a fastly log with empty space in filename" in {

    val fastlyLog3 = fastlyLog1.copy(url = "/2018/01/09-50280-01 gnl.business.20180109.candb.goingglobal.mp3")

    Event(fastlyLog3) should be(Some(Event(
      viewId = "8813254641924280511",
      url = "https://audio.guim.co.uk/2018/01/09-50280-01%20gnl.business.20180109.candb.goingglobal.mp3",
      ipAddress = "66.87.114.159",
      episodeId = "https://www.theguardian.com/small-business-network/audio/2018/jan/10/bedroom-business-to-world-domination-meet-the-mentors-with-trunkis-rob-law-podcast",
      podcastId = "https://www.theguardian.com/business/series/the-business-podcast",
      ua = "Samsung SM-G900P stagefright/Beyonce/1.1.9 (Linux;Android 6.0.1)")))
  }

  it should "Convert a fastly log with '+' in filename" in {

    val fastlyLog3 = fastlyLog1.copy(url = "/2018/07/12-76884-BreathlessEP01_MIX1_+MP.mp3")

    Event(fastlyLog3) should be(Some(Event(
      viewId = "-2452628349673661344",
      url = "https://audio.guim.co.uk/2018/07/12-76884-BreathlessEP01_MIX1_+MP.mp3",
      ipAddress = "66.87.114.159",
      episodeId = "https://www.theguardian.com/australia-news/audio/2018/jul/13/breathless-episode-1-death-david-dungay-jr-podcast",
      podcastId = "https://www.theguardian.com/australia-news/series/breathless-the-death-of-david-dungay-jnr",
      ua = "Samsung SM-G900P stagefright/Beyonce/1.1.9 (Linux;Android 6.0.1)")))
  }

  it should "When converting a FastlyLog to an Event, it should filter out all the 206 partial content statuses" in {

    val logs: Seq[FastlyLog] = Source.fromFile("src/test/resources/logs-sample.txt")("ISO-8859-1").getLines().toSeq flatMap { line => FastlyLog(line) }

    val logsGrouped = logs.groupBy(f => f.status).view.mapValues(_.length)

    logsGrouped.get("206").value should be(71)

    logsGrouped.get("200").value should be(12)
    logsGrouped.get("304").value should be(3)

    val events = logs.filter(FastlyLog.onlyDownloads).flatMap(Event(_))

    events.length should be(26)

  }

  it should "When converting a FastlyLog to an Event, it should filter out all the HEAD requests" in {

    val fastlyLog2 = fastlyLog1.copy(request = "HEAD")

    val logs: Seq[FastlyLog] = List(fastlyLog1, fastlyLog2)

    val events = logs.filter(FastlyLog.onlyGet).flatMap(Event(_))

    events.length should be(1)
  }
}