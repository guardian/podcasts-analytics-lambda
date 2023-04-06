package com.gu.contentapi

import com.gu.contentapi.models.{ Event, AcastLog }
import com.gu.contentapi.services.PodcastLookup
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.OptionValues
import org.scalatest.matchers.should._

import scala.io.Source

class AcastParsingSpec extends AnyFlatSpec with Matchers with OptionValues {

  it should "parse a log file into a Acast log model" in {

    val lines: Seq[String] = Source.fromFile("src/test/resources/acast-logs-sample.txt")("ISO-8859-1").getLines().toSeq

    val downloads: Seq[AcastLog] = lines flatMap { line => AcastLog(line) }

    lines.length should be(4)
    downloads.length should be(4)

    val first = AcastLog(
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
    downloads.head should be(first)

    val second = AcastLog(
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
    downloads(1) should be(second)

    val third = AcastLog(
      timestamp = "2017-05-18T07:18:13.000Z",
      bytes = "26969867",
      range = "-",
      ipAddress = "101.186.58.250",
      request = "GET",
      url = "/encoded/dcd5264b-a4bc-4fc6-a7d7-23500e04fffd/9f9a7fd1-6f8c-41dc-bb9c-bac2b7d5b31c/encoded-0a2236c4665171be111f0bd8254e29e7-128.mp3",
      status = "206",
      referrer = "-",
      userAgent = "AppleCoreMedia/1.0.0.14D27 (iPhone; U; CPU OS 10_2_1 like Mac OS X; en_gb)",
      query = "ci=991ebf3d-15cf-4c8f-9359-2f4cd40880f3&aid=9f9a7fd1-6f8c-41dc-bb9c-bac2b7d5b31c&chid=dcd5264b-a4bc-4fc6-a7d7-23500e04fffd&st=2__Gg6W_YexJ3eSCmTnAJA&e=1495107145&filename=The-Guardians-Science-Weekly-Science-weekly-can-we-cure-Alzheimers-podcast.mp3",
      host = "mediacdn.acast.com",
      forwardedFor = "-",
      cloudfrontResultType = "Hit",
      cloudfrontResponseResultType = "Hit",
      cloudfrontRequestId = "i5cq2cAbWqkeCGwYVN0Ss-hmgxjaA4bH43OoEzEzdFvDdlDdJ1wjYA==",
      city = "Perth",
      country = "Australia",
      countryCode = "AU",
      region = "WA",
      continentCode = "OC",
      dmaCode = "0",
      postalCode = "6000",
      longitude = "115.8621",
      latitude = "-31.9674",
      filename = "https://audio.guim.co.uk/2017/05/12-48386-gnl.rs.20170521.scienceweekly.alzheimers.mp3")
    downloads(2) should be(third)

    val fourth = AcastLog(
      timestamp = "2017-05-18T07:18:13.000Z",
      bytes = "26969867",
      range = "-",
      ipAddress = "101.186.58.250",
      request = "GET",
      url = "/encoded/dcd5264b-a4bc-4fc6-a7d7-23500e04fffd/9f9a7fd1-6f8c-41dc-bb9c-bac2b7d5b31c/encoded-0a2236c4665171be111f0bd8254e29e7-128.mp3",
      status = "206",
      referrer = "-",
      userAgent = "AppleCoreMedia/1.0.0.14D27 (iPhone; U; CPU OS 10_2_1 like Mac OS X; en_gb)",
      query = "ci=991ebf3d-15cf-4c8f-9359-2f4cd40880f3&aid=9f9a7fd1-6f8c-41dc-bb9c-bac2b7d5b31c&chid=dcd5264b-a4bc-4fc6-a7d7-23500e04fffd&st=2__Gg6W_YexJ3eSCmTnAJA&e=1495107145&filename=The-Guardians-Science-Weekly-Science-weekly-can-we-cure-Alzheimers-podcast.mp3",
      host = "mediacdn.acast.com",
      forwardedFor = "-",
      cloudfrontResultType = "Hit",
      cloudfrontResponseResultType = "Hit",
      cloudfrontRequestId = "i5cq2cAbWqkeCGwYVN0Ss-hmgxjaA4bH43OoEzEzdFvDdlDdJ1wjYA==",
      city = "Perth",
      country = "Australia",
      countryCode = "AU",
      region = "WA",
      continentCode = "OC",
      dmaCode = "0",
      postalCode = "6000",
      longitude = "115.8621",
      latitude = "-31.9674",
      filename = "https://audio.guim.co.uk/2017/05/12-48386-gnl.rs.20170521.scienceweekly .alzheimers.mp3")
    downloads(3) should be(fourth)

  }

}