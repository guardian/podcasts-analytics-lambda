package com.gu.contentapi

import com.amazonaws.services.lambda.runtime.events.S3Event
import com.amazonaws.services.lambda.runtime.{ Context, RequestHandler }
import com.amazonaws.services.s3.event.S3EventNotification.{ S3Entity, S3EventNotificationRecord }
import com.amazonaws.services.s3.model.S3Object

import scala.collection.JavaConverters._
import com.gu.contentapi.models.{ AcastLog, Event, FastlyLog }
import com.gu.contentapi.services.{ Ophan, PodcastLookup, S3 }
import com.gu.contentapi.utils.LogProcessor

import org.apache.logging.log4j.scala.Logging

class Lambda extends RequestHandler[S3Event, Unit] with Logging {

  override def handleRequest(event: S3Event, context: Context): Unit = {

    val (fastlyRecords, maybeAcastRecords) = event.getRecords.asScala
      .partition(rec => isLogType(Config.FastlyAudioLogsBucketName, rec.getS3))
    val (acastRecords, otherRecords) = maybeAcastRecords
      .partition(rec => isLogType(Config.AcastAudioLogsBucketName, rec.getS3))

    Ophan.send {
      LogProcessor.fastly.process(fastlyRecords).flatMap(Event(_))
    }

    Ophan.send {
      LogProcessor.acast.process(acastRecords).flatMap(Event(_))
    }

    logger.debug(s"Cache hits: ${PodcastLookup.cacheHits}")
    logger.debug(s"Cache misses: ${PodcastLookup.cacheMisses}")
  }

  def isLogType(typeName: String, s3Entity: S3Entity): Boolean =
    s3Entity.getBucket.getName == typeName ||
      s3Entity.getObject.getKey.startsWith(typeName)

}
