package com.gu.contentapi

import com.amazonaws.services.lambda.runtime.events.S3Event
import com.amazonaws.services.lambda.runtime.{ Context, RequestHandler }
import com.amazonaws.services.s3.event.S3EventNotification.{ S3Entity, S3EventNotificationRecord }
import com.amazonaws.services.s3.model.S3Object

import scala.collection.mutable.Buffer
import scala.collection.GenTraversableOnce
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

    process[FastlyLog](r => FastlyLog.onlyDownloads(r) && FastlyLog.onlyGet(r), Event(_), FastlyLog(_))(fastlyRecords)
    process[AcastLog](r => AcastLog.onlyDownloads(r) && AcastLog.onlySuccessfulReponses(r) && AcastLog.onlyGet(r), Event(_), AcastLog(_))(acastRecords)

    logger.debug(s"Cache hits: ${PodcastLookup.cacheHits}")
    logger.debug(s"Cache misses: ${PodcastLookup.cacheMisses}")
  }

  private final def process[A](
    predicate: A => Boolean,
    toEvent: A => GenTraversableOnce[Event],
    builder: String => GenTraversableOnce[A])(objects: Buffer[S3EventNotificationRecord]): Unit =
    objects.toList
      .map(S3.downloadReport(_).map(toDownloadLogs[A](predicate, builder)).getOrElse(Nil))
      .foreach(downloadsLogs => Ophan.send(downloadsLogs.flatMap(toEvent)))

  private def isLogType(typeName: String, s3Entity: S3Entity): Boolean =
    s3Entity.getBucket.getName == typeName ||
      s3Entity.getObject.getKey.startsWith(typeName)

  private def toDownloadLogs[A](predicate: A => Boolean, builder: String => GenTraversableOnce[A])(obj: S3Object): List[A] = {
    Source
      .fromBytes(IOUtils.toByteArray(obj.getObjectContent))("ISO-8859-1")
      .getLines
      .toList
      .flatMap(builder)
      .filter(predicate)
  }

}
