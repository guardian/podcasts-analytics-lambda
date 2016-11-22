package com.gu.contentapi

import com.gu.contentapi.models.FastlyLog
import org.scalatest.{ FlatSpec, Matchers }
import scala.io.Source

class LambdaSpec extends FlatSpec with Matchers {

  it should "parse a log file into a FastlyView model" in {

    val pageViews: Seq[FastlyLog] = Source.fromFile("src/test/resources/logs-sample.txt").getLines().toSeq flatMap { line =>
      FastlyLog(line)
    }

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

}