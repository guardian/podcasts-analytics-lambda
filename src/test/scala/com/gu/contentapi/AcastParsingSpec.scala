package com.gu.contentapi

import com.gu.contentapi.models.{ Event, AcastLog }
import com.gu.contentapi.services.PodcastLookup
import org.scalatest.{ FlatSpec, Matchers, OptionValues }

import scala.io.Source

class AcastParsingSpec extends FlatSpec with Matchers with OptionValues {

  it should "parse a log file into a Acast log model" in {

    val downloads: Seq[AcastLog] = Source.fromFile("src/test/resources/acast-logs-sample.txt")("ISO-8859-1").getLines().toSeq flatMap { line =>
      AcastLog(line)
    }

    downloads.length should be(137)

    val first = AcastLog(
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
    downloads.head should be(first)

    val second = AcastLog(
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
      latitude = "-31.9674"
    )
    downloads(1) should be(second)

  }

}