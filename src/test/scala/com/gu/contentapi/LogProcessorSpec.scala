package com.gu.contentapi.utils

import com.gu.contentapi.models.{ Event, AcastLog }
import com.gu.contentapi.services.PodcastLookup
import java.io.FileInputStream
import org.scalatest.{ FlatSpec, Matchers, OptionValues }

import scala.io.Source

class LogProcessorSpec extends FlatSpec with Matchers with OptionValues {

  it should "isolate uniques in Acast logs" in {
    val input = new FileInputStream(getClass.getResource("/acast-logs-sample.txt").getFile)

    val result: List[AcastLog] = LogProcessor.toDownloadLogs[AcastLog](
      r => AcastLog.onlyDownloads(r) && AcastLog.onlySuccessfulReponses(r) && AcastLog.onlyGet(r),
      AcastLog(_))(input)

    val filteredResult: List[AcastLog] = LogProcessor.acast.processAcast(result)

    result should have length 3
    filteredResult should have length 2
  }

}