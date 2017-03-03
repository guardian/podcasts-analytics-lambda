package com.gu.contentapi

import com.gu.contentapi.models.{ Event, FastlyLog }
import com.gu.contentapi.services.PodcastLookup
import org.scalatest.{ FlatSpec, Matchers, OptionValues }

import scala.io.Source

class LambdaSpec extends FlatSpec with Matchers with OptionValues {

  it should "parse a log file into a FastlyView model" in {

    val pageViews: Seq[FastlyLog] = Source.fromFile("src/test/resources/logs-sample.txt")("ISO-8859-1").getLines().toSeq flatMap { line =>
      FastlyLog(line)
    }

    pageViews.length should be(86)

    val firstPw = FastlyLog(
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
    firstPw should be(pageViews.head)

    val secondPw = FastlyLog(
      time = "Fri, 11 Nov 2016 13:00:00 GMT",
      request = "GET",
      url = "/2016/11/10-44199-gdn.lr.161110.sb.the-cult-of-the-expert.mp3",
      host = "composer-uploads-audio.s3-website-eu-west-1.amazonaws.com",
      status = "206",
      ipAddress = "185.51.75.28",
      userAgent = "AppleCoreMedia/1.0.0.14A456 (iPhone; U; CPU OS 10_0_2 like Mac OS X; en_ie)",
      referrer = "",
      range = "bytes=0-34238286",
      headerBytesRead = "365",
      bodyBytesRead = "0",
      contentType = "audio/mp3",
      age = "1977",
      countryCode = "IE",
      region = "07",
      city = "Dublin",
      location = ""
    )
    secondPw should be(pageViews(1))

    val thirdPw = FastlyLog(
      time = "Fri, 11 Nov 2016 13:00:01 GMT",
      request = "GET",
      url = "/2016/11/10-58860-FW-10nov-2016_mixdown.mp3",
      host = "composer-uploads-audio.s3-website-eu-west-1.amazonaws.com",
      status = "206",
      ipAddress = "149.254.51.142",
      userAgent = "AppleCoreMedia/1.0.0.14B100 (iPhone; U; CPU OS 10_1_1 like Mac OS X; en_gb)",
      referrer = "",
      range = "bytes=60882944-61458334",
      headerBytesRead = "354",
      bodyBytesRead = "0",
      contentType = "audio/mp3",
      age = "2309",
      countryCode = "GB",
      region = "A2",
      city = "Hendon",
      location = ""
    )
    thirdPw should be(pageViews(2))
  }

  //TODO: the following tests should be treated as integration tests rather than being commented out

  it should "Test the Podcast Lookup function" in {

    val info = PodcastLookup.getPodcastInfo("https://audio.guim.co.uk/2016/12/19-57926-FW-dec19-2016_mixdown.mp3")

    info.get.podcastId should be("https://www.theguardian.com/football/series/footballweekly")
    info.get.episodeId should be("https://www.theguardian.com/football/blog/audio/2016/dec/19/manchester-city-bounce-back-to-leave-wenger-fuming-football-weekly")
  }

  val log1 = FastlyLog(
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

    Event(log1) should be(Some(Event(
      viewId = "-1969270942156082233",
      url = "https://audio.guim.co.uk/2016/11/10-58860-FW-10nov-2016_mixdown.mp3",
      ipAddress = "66.87.114.159",
      episodeId = "https://www.theguardian.com/football/audio/2016/nov/10/gordon-strachans-last-stand-with-scotland-football-weekly-extra",
      podcastId = "https://www.theguardian.com/football/series/footballweekly",
      ua = "Samsung SM-G900P stagefright/Beyonce/1.1.9 (Linux;Android 6.0.1)"
    )))
  }

  it should "Handle platform parameter correctly" in {

    val log2 = log1.copy(url = log1.url + "?platform=amazon-echo")

    Event(log2) should be(Some(Event(
      viewId = "-1969270942156082233",
      url = "https://audio.guim.co.uk/2016/11/10-58860-FW-10nov-2016_mixdown.mp3",
      ipAddress = "66.87.114.159",
      episodeId = "https://www.theguardian.com/football/audio/2016/nov/10/gordon-strachans-last-stand-with-scotland-football-weekly-extra",
      podcastId = "https://www.theguardian.com/football/series/footballweekly",
      ua = "Samsung SM-G900P stagefright/Beyonce/1.1.9 (Linux;Android 6.0.1)",
      platform = Some("amazon-echo")
    )))
  }

  it should "send a single podcast to Ophan" in {

    val ev = Event(
      viewId = "-5103960900567454554",
      url = "https://audio.guim.co.uk/2016/11/10-58860-FW-10nov-2016_mixdown.mp3",
      ipAddress = "66.87.114.159",
      episodeId = "https://www.theguardian.com/football/audio/2016/nov/10/gordon-strachans-last-stand-with-scotland-football-weekly-extra",
      podcastId = "https://www.theguardian.com/football/series/footballweekly",
      ua = "Samsung SM-G900P stagefright/Beyonce/1.1.9 (Linux;Android 6.0.1)"
    )
    //    Ophan.send(ev)
  }

  it should "when converting a FastlyLog to an Event, it should filter out all the 206 partial content statuses" in {

    val logs: Seq[FastlyLog] = Source.fromFile("src/test/resources/logs-sample.txt")("ISO-8859-1").getLines().toSeq flatMap { line => FastlyLog(line) }

    val logsGrouped = logs.groupBy(f => f.status).mapValues(_.length)

    logsGrouped.get("206").value should be(71)
    logsGrouped.get("200").value should be(12)
    logsGrouped.get("304").value should be(3)

    val events = logs.filter(FastlyLog.onlyDownloads).flatMap(Event(_))

    events.length should be(15)

  }

}