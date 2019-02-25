package com.gu.contentapi

import com.amazonaws.services.lambda.runtime.events.S3Event
import com.amazonaws.services.lambda.runtime.{ Context, RequestHandler }
import com.amazonaws.services.s3.event.S3EventNotification.{ S3Entity, S3EventNotificationRecord }
import com.amazonaws.services.s3.model.S3Object

import scala.collection.mutable.Buffer
import scala.collection.JavaConverters._
import com.gu.contentapi.models.{ AcastLog, Event, FastlyLog }
import com.gu.contentapi.services.{ Ophan, PodcastLookup, S3 }
import com.amazonaws.util.IOUtils

import scala.io.Source
import org.apache.logging.log4j.scala.Logging

class Lambda extends RequestHandler[S3Event, Unit] with Logging {

  override def handleRequest(event: S3Event, context: Context): Unit = {

    val (fastlyRecords, maybeAcastRecords) = event.getRecords.asScala
      .partition(rec => isLogType(Config.FastlyAudioLogsBucketName, rec.getS3))
    val (acastRecords, otherRecords) = maybeAcastRecords
      .partition(rec => isLogType(Config.AcastAudioLogsBucketName, rec.getS3))

    Ophan.send {
      process[FastlyLog](FastlyLog(_), fastlyRecords) {
        r => FastlyLog.onlyDownloads(r) && FastlyLog.onlyGet(r)
      }.flatMap(Event(_))
    }

    Ophan.send {
      val unfilteredLogs = process[AcastLog](AcastLog(_), acastRecords) {
        r => AcastLog.onlyDownloads(r) && AcastLog.onlySuccessfulReponses(r) && AcastLog.onlyGet(r)
      }
      processAcast(unfilteredLogs).flatMap(Event(_))
    }

    logger.debug(s"Cache hits: ${PodcastLookup.cacheHits}")
    logger.debug(s"Cache misses: ${PodcastLookup.cacheMisses}")
  }

  private final def process[A](builder: String => Option[A], objects: Buffer[S3EventNotificationRecord])(predicate: A => Boolean): List[A] =
    objects.toList
      .flatMap(S3.downloadReport(_).map(toDownloadLogs[A](predicate, builder)).getOrElse(Nil))

  /**
   * Logs are aggregated according to three pieces of information: the user agent, the ip address
   * and the episode url: this is as close as we can get to a unique. For each group, we sum the
   * number of bytes being sent back and filter out entries that do not go beyond
   * `Config.minDownloadSize` bytes.
   */
  private final def processAcast[A](logs: List[AcastLog]): List[AcastLog] =
    logs.foldLeft(Map.empty[(String, String, String), (AcastLog, Long)]) {
      case (entries, log) =>
        val size = log.bytes
        val key = (log.ipAddress, log.userAgent, log.url)
        entries + entries.get(key).foldLeft(key -> (log, size)) {
          case ((key, (log, size)), entry) =>
            key -> (log, size + entry._2)
        }
    }
      .values
      .filter(_._2 >= Config.minDownloadSize)
      .map(_._1)
      .toList

  private def isLogType(typeName: String, s3Entity: S3Entity): Boolean =
    s3Entity.getBucket.getName == typeName ||
      s3Entity.getObject.getKey.startsWith(typeName)

  private def toDownloadLogs[A](predicate: A => Boolean, builder: String => Option[A])(obj: S3Object): List[A] = {
    Source
      .fromBytes(IOUtils.toByteArray(obj.getObjectContent))("ISO-8859-1")
      .getLines
      .toList
      .flatMap(l => builder(l).map(List(_)).getOrElse(Nil))
      .filter(predicate)
  }

}
