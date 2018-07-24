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

    val logsGrouped = logs.groupBy(f => f.status).mapValues(_.length)

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
    latitude = "41.9",
    filename = "https://audio.guim.co.uk/2017/05/12-48386-gnl.rs.20170521.scienceweekly.alzheimers.mp3")

  val acastLog2 = AcastLog(
    timestamp = "2017-05-29T11:00:06.000Z",
    bytes = "18195051",
    range = "-",
    ipAddress = "217.42.5.77",
    request = "GET",
    url = "/stitch1/2017/05/guardian-science/the-bell-beaker-folk-science-weekly-podcast/7g49LLBQMKaqWB-FvDHpFw.mp3",
    status = "206",
    referrer = "-",
    userAgent = "AppleCoreMedia/1.0.0.14E304 (iPhone; U; CPU OS 10_3_1 like Mac OS X; en_gb)",
    query = "ci=c1859bb5-4cd3-47ff-adeb-f0ab3b999f1e&aid=4b866423-bbd8-49b7-87e0-d53c883b6ea5&chid=dcd5264b-a4bc-4fc6-a7d7-23500e04fffd&st=5rYBLVWUGQei6khP3xD33g&e=1496070746&filename=The-Guardians-Science-Weekly-The-Bell-Beaker-folk-Science-Weekly-podcast.mp3",
    host = "ads-creative-cdn.acast.com",
    forwardedFor = "-",
    cloudfrontResultType = "Hit",
    cloudfrontResponseResultType = "Hit",
    cloudfrontRequestId = "PRr0SRHyeN0RCw1jzZWpW5iwPO8fH37Xu9OChM17LwqK5YBtb2UdFw==",
    city = "Durham",
    country = "United Kingdom",
    countryCode = "GB",
    region = "DUR",
    continentCode = "EU",
    dmaCode = "0",
    postalCode = "DH9",
    longitude = "-1.7383",
    latitude = "54.8575",
    filename = "https://audio.guim.co.uk/2017/05/26-52950-gdn.sci.20170526.gj.ancientgenomes.mp3")

  it should "Convert a acast log with a filename to an event ready to be sent to Ophan" in {

    Event(acastLog2) should be(Some(Event(
      viewId = "PRr0SRHyeN0RCw1jzZWpW5iwPO8fH37Xu9OChM17LwqK5YBtb2UdFw==",
      url = "https://audio.guim.co.uk/2017/05/26-52950-gdn.sci.20170526.gj.ancientgenomes.mp3",
      ipAddress = "217.42.5.77",
      episodeId = "https://www.theguardian.com/science/audio/2017/may/28/the-bell-beaker-folk-science-weekly-podcast",
      podcastId = "https://www.theguardian.com/science/series/science",
      ua = "AppleCoreMedia/1.0.0.14E304 (iPhone; U; CPU OS 10_3_1 like Mac OS X; en_gb)")))
  }

  it should "Convert a acast log with a filename containing an space" in {

    val acastLog3 = acastLog2.copy(filename = "https://audio.guim.co.uk/2018/01/09-50280-01 gnl.business.20180109.candb.goingglobal.mp3")

    Event(acastLog3) should be(Some(Event(
      viewId = "PRr0SRHyeN0RCw1jzZWpW5iwPO8fH37Xu9OChM17LwqK5YBtb2UdFw==",
      url = "https://audio.guim.co.uk/2018/01/09-50280-01 gnl.business.20180109.candb.goingglobal.mp3",
      ipAddress = "217.42.5.77",
      episodeId = "https://www.theguardian.com/small-business-network/audio/2018/jan/10/bedroom-business-to-world-domination-meet-the-mentors-with-trunkis-rob-law-podcast",
      podcastId = "https://www.theguardian.com/business/series/the-business-podcast",
      ua = "AppleCoreMedia/1.0.0.14E304 (iPhone; U; CPU OS 10_3_1 like Mac OS X; en_gb)")))
  }

  it should "When converting a AcastLog to an Event, it should filter out all the HEAD requests" in {

    val acastLog3 = acastLog2.copy(request = "HEAD")

    val logs: Seq[AcastLog] = List(acastLog1, acastLog2, acastLog3)

    val events = logs.filter(AcastLog.onlyGet).flatMap(Event(_))

    events.length should be(2)
  }

  it should "When converting a AcastLog to an Event, it should filter out 206 requests unless the byte-range starts from 0 and is not 0-1" in {

    val acastLog3 = acastLog1.copy(status = "206", range = "0-10", userAgent = "acastLog3")
    val acastLog4 = acastLog1.copy(status = "206", range = "0-11", userAgent = "acastLog4")
    val acastLog5 = acastLog1.copy(status = "206", range = "0-1", userAgent = "acastLog5") //excluded

    val logs: Seq[AcastLog] = List(acastLog1, acastLog3, acastLog4, acastLog5)

    val events = logs.filter(AcastLog.onlyDownloads).flatMap(Event(_))

    events.length should be(3)
    events.map(_.ua) should be(Seq(acastLog1.userAgent, acastLog3.userAgent, acastLog4.userAgent))
  }

  it should "When converting a AcastLog to an Event, it should  filter out  206 requests starting with a number higher than 0 " in {

    val acastLog3 = acastLog1.copy(status = "206", range = "12-235")

    val logs: Seq[AcastLog] = List(acastLog1, acastLog3)

    val events = logs.filter(AcastLog.onlyDownloads).flatMap(Event(_))

    events.length should be(1)
  }

}