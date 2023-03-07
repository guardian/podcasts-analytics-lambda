package com.gu.contentapi

import com.amazonaws.services.lambda.runtime.events.S3Event
import com.amazonaws.services.lambda.runtime.{Context, RequestHandler}
import com.amazonaws.services.s3.event.S3EventNotification.S3Entity
import com.amazonaws.services.s3.model.S3Object

import scala.collection.JavaConverters._
import com.gu.contentapi.models.{AcastLog, Event, FastlyLog}
import com.gu.contentapi.services.{Ophan, PodcastLookup, S3}
import com.amazonaws.util.IOUtils

import scala.io.Source
import org.apache.logging.log4j.scala.Logging

class Lambda extends RequestHandler[S3Event, Unit] with Logging {

  override def handleRequest(event: S3Event, context: Context): Unit = {

    val fastlyReportObjects = event.getRecords.asScala
      .filter(rec => isLogType(Config.FastlyAudioLogsBucketName, rec.getS3))
      .flatMap(rec => S3.downloadReport(rec.getS3.getBucket.getName, rec.getS3.getObject.getKey.replaceAll("%3A", ":"))) // looks like the json object is not decoded properly by the SKD
      .toList

    val acastReportObjects = event.getRecords.asScala
      .filter(rec => isLogType(Config.AcastAudioLogsBucketName, rec.getS3))
      .flatMap(rec => S3.downloadReport(rec.getS3.getBucket.getName, rec.getS3.getObject.getKey.replaceAll("%3A", ":"))) // looks like the json object is not decoded properly by the SKD
      .toList

    handleReportsFromFastly(fastlyReportObjects)

    handleReportsFromAcast(acastReportObjects)

    logger.debug(s"Cache hits: ${PodcastLookup.cacheHits}")
    logger.debug(s"Cache misses: ${PodcastLookup.cacheMisses}")

  }

  private def isLogType(typeName: String, s3Entity: S3Entity): Boolean =
    s3Entity.getBucket.getName == typeName ||
      s3Entity.getObject.getKey.startsWith(typeName)

  private def handleReportsFromFastly(objects: List[S3Object]): Unit = {
    objects foreach { obj =>

      val allFastlyLogs = Source.fromBytes(IOUtils.toByteArray(obj.getObjectContent))("ISO-8859-1").getLines().toSeq flatMap { line =>
        FastlyLog(line)
      }

      import com.gu.contentapi.utils.PredicateUtils._

      val downloadsLogs = allFastlyLogs.filter(FastlyLog.onlyDownloads && FastlyLog.onlyGet)

      Ophan.send(downloadsLogs.flatMap(Event(_)))
    }
  }

  private def handleReportsFromAcast(objects: List[S3Object]): Unit = {
    objects foreach { obj =>

      val allAcastLogs = Source.fromBytes(IOUtils.toByteArray(obj.getObjectContent))("ISO-8859-1").getLines().toSeq flatMap { line =>
        AcastLog(line)
      }

      import com.gu.contentapi.utils.PredicateUtils._

      val downloadsLogs = allAcastLogs.filter(AcastLog.onlyDownloads && AcastLog.onlySuccessfulReponses && AcastLog.onlyGet)

      Ophan.send(downloadsLogs.flatMap(Event(_)))
    }
  }

}
