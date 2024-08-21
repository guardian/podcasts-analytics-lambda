package com.gu.contentapi

import com.amazonaws.services.lambda.runtime.events.S3Event
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification
import com.amazonaws.services.lambda.runtime.{ Context, RequestHandler }

import scala.jdk.CollectionConverters._
import com.gu.contentapi.models.{ AcastLog, Event, FastlyLog }
import com.gu.contentapi.services.{ Loader, Ophan, PodcastLookup, S3 }
import org.apache.logging.log4j.scala.Logging
import com.gu.contentapi.utils.PredicateUtils._
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import scala.concurrent.{ Await, Future }
import scala.util.{ Failure, Success }
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

class Lambda extends RequestHandler[S3Event, Unit] with Logging {

  override def handleRequest(event: S3Event, context: Context): Unit = {

    val fastlyReportObjects = Await.result(
      Future.sequence(
        event.getRecords.asScala
          .filter(rec => isLogType(Config.FastlyAudioLogsBucketName, rec.getS3))
          .map(rec => S3.downloadReport(rec.getS3.getBucket.getName, rec.getS3.getObject.getKey.replaceAll("%3A", ":"))) // looks like the json object is not decoded properly by the SKD
      ).map(_.toList),
      Duration(context.getRemainingTimeInMillis, TimeUnit.MILLISECONDS))

    handleReportsFromFastly(fastlyReportObjects.collect({ case Some(path) => path }))

    val acastReportObjects = Await.result(
      Future.sequence(
        event.getRecords.asScala
          .filter(rec => isLogType(Config.AcastAudioLogsBucketName, rec.getS3))
          .map(rec => S3.downloadReport(rec.getS3.getBucket.getName, rec.getS3.getObject.getKey.replaceAll("%3A", ":"))) // looks like the json object is not decoded properly by the SKD
      ).map(_.toList),
      Duration(context.getRemainingTimeInMillis, TimeUnit.MILLISECONDS))

    handleReportsFromAcast(acastReportObjects.collect({ case Some(path) => path }))

    logger.debug(s"Cache hits: ${PodcastLookup.cacheHits}")
    logger.debug(s"Cache misses: ${PodcastLookup.cacheMisses}")

  }

  private def isLogType(typeName: String, s3Entity: S3EventNotification.S3Entity): Boolean =
    s3Entity.getBucket.getName == typeName ||
      s3Entity.getObject.getKey.startsWith(typeName)

  private def handleReportsFromFastly(objects: List[Path]): Unit = {
    objects foreach { obj =>
      Loader.linesFromFile(obj).map(_.flatMap(FastlyLog.apply)) match {
        case Success(allFastlyLogs) =>

          val downloadsLogs = allFastlyLogs.filter(FastlyLog.onlyDownloads && FastlyLog.onlyGet)
          Ophan.send(downloadsLogs.flatMap(Event(_)))

        case Failure(err) =>
          logger.error(s"Could not load fastly logs from tempfile ${obj.toString}: ${err.getMessage}", err)
      }
    }
  }

  private def handleReportsFromAcast(objects: List[Path]): Unit = {
    objects foreach { obj =>
      Loader.linesFromFile(obj).map(_.flatMap(AcastLog.apply(_))) match {
        case Success(allAcastLogs) =>
          val downloadsLogs = allAcastLogs.filter(AcastLog.onlyDownloads && AcastLog.onlySuccessfulReponses && AcastLog.onlyGet)

          Ophan.send(downloadsLogs.flatMap(Event(_)))
        case Failure(err) =>
          logger.error(s"Could not load acast logs from tempfile ${obj.toString}: ${err.getMessage}", err)
      }
    }
  }

}
