package com.gu.contentapi

import com.amazonaws.services.lambda.runtime.events.S3Event
import com.amazonaws.services.lambda.runtime.{ Context, RequestHandler }
import com.amazonaws.services.s3.model.S3Object
import scala.collection.JavaConverters._
import com.gu.contentapi.models.{ Event, AcastLog, FastlyLog }
import com.gu.contentapi.services.{ PodcastLookup, S3, Ophan }
import com.typesafe.scalalogging.StrictLogging
import com.amazonaws.util.IOUtils
import scala.io.Source

class Lambda extends RequestHandler[S3Event, Unit] with StrictLogging {

  override def handleRequest(event: S3Event, context: Context): Unit = {

    val fastlyReportObjects = event.getRecords.asScala
      .filter(_.getS3.getBucket.getName == Config.FastlyAudioLogsBucketName)
      .flatMap(reportObject => S3.downloadReport(Config.FastlyAudioLogsBucketName, reportObject.getS3.getObject.getKey.replaceAll("%3A", ":"))) // looks like the json object is not decoded properly by the SKD
      .toList

    val acastReportObjects = event.getRecords.asScala
      .filter(_.getS3.getBucket.getName == Config.AcastAudioLogsBucketName)
      .flatMap(reportObject => S3.downloadReport(Config.AcastAudioLogsBucketName, reportObject.getS3.getObject.getKey.replaceAll("%3A", ":"))) // looks like the json object is not decoded properly by the SKD
      .toList

    handleReportsFromFastly(fastlyReportObjects)

    handleReportsFromAcast(fastlyReportObjects)

    println(s"Cache hits: ${PodcastLookup.cacheHits}")
    println(s"Cache misses: ${PodcastLookup.cacheMisses}")

  }

  private def handleReportsFromFastly(objects: List[S3Object]): Unit = {
    objects foreach { obj =>

      val allFastlyLogs = Source.fromBytes(IOUtils.toByteArray(obj.getObjectContent))("ISO-8859-1").getLines().toSeq flatMap { line =>
        FastlyLog(line)
      }

      val downloadsLogs = allFastlyLogs.filter(FastlyLog.onlyDownloads)

      Ophan.send(downloadsLogs.flatMap(Event(_)))
    }
  }

  private def handleReportsFromAcast(objects: List[S3Object]): Unit = {
    objects foreach { obj =>

      val allAcastLogs = Source.fromBytes(IOUtils.toByteArray(obj.getObjectContent))("ISO-8859-1").getLines().toSeq flatMap { line =>
        AcastLog(line)
      }

      import com.gu.contentapi.utils.PredicateUtils._

      val downloadsLogs = allAcastLogs.filter(AcastLog.onlyDownloads && AcastLog.onlySuccessfulReponses)

      /**
       *
       * Do not send event yet just print them for testing
       *
       * Ophan.send(downloadsLogs.flatMap(Event(_)))
       */
      val events = downloadsLogs.flatMap(Event(_))
      for (event <- events) {
        println("Acast event")
        println(event)
      }
    }
  }

}