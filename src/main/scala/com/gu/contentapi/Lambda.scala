package com.gu.contentapi

import com.amazonaws.services.lambda.runtime.events.S3Event
import com.amazonaws.services.lambda.runtime.{Context, RequestHandler}
import com.amazonaws.services.s3.event.S3EventNotification.S3Entity
import com.amazonaws.services.s3.model.S3Object

import scala.collection.JavaConverters._
import com.gu.contentapi.models.{AcastLog, AcastIabLog, Event, FastlyLog}
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

    val acastIabReportObjects = event.getRecords.asScala
      .filter(rec => isLogType(Config.AcastIabAudioLogsBucketName, rec.getS3))
      .flatMap(rec => S3.downloadReport(rec.getS3.getBucket.getName, rec.getS3.getObject.getKey.replaceAll("%3A", ":"))) // looks like the json object is not decoded properly by the SKD
      .toList

    handleReportsFromFastly(fastlyReportObjects)

    handleReportsFromAcast(acastReportObjects)

    handleIabReportsFromAcast(acastIabReportObjects)

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

  private def handleIabReportsFromAcast(objects: List[S3Object]): Unit = {

    val IabValidThreshold = 1000

    objects foreach { obj =>

      // get all the log entries from the file
      val allAcastIabLogs = Source.fromBytes(IOUtils.toByteArray(obj.getObjectContent))("ISO-8859-1").getLines().toSeq flatMap { line =>
        AcastIabLog(line)
      }

      // filter down to just successful 200 requests - these are the entry points and may or may not be followed
      // by a number of 206 requests
      val allUserInitialRequests = allAcastIabLogs
        .filter(AcastIabLog.successfulInitialRequestsFilter)

      // distinct list of user id hashes from those 200 requests
      val userIdHashes = allUserInitialRequests
        .groupBy(_.userIdHash)
        .keys

      // for each user and distinct file they requested, sum the bytes delivered for each file,
      // and set the isIabValid field to "true" if the bytes delivered value is greater than the threshold
      // otherwise isIabValid is false
      val distinctUserFileRequestsWithAggregatedBytesAndIabMarker: Seq[AcastIabLog] = userIdHashes.flatMap { userId =>
        val userRequests = AcastIabLog.userRequests(allAcastIabLogs, userId)
        val userFileRequests = userRequests.groupBy(_.sourceFileName)

        userFileRequests.flatMap { fr =>
          val sourceFileName = fr._1
          val fileRequests = fr._2
          val bytesDelivered = fileRequests.foldLeft(0)(_ + _.bytesDelivered.toInt)
          userRequests.collect({
            case req if req.userIdHash == userId &&
                        req.httpStatus == "200" &&
                        req.sourceFileName == sourceFileName => req
          }).map { logEntry =>
            val isIabValid = bytesDelivered >= IabValidThreshold
            logEntry.copy(bytesDelivered = bytesDelivered.toString, isIabValid = isIabValid.toString)
          }
        }
      }.toSeq

      val ophanEvents = distinctUserFileRequestsWithAggregatedBytesAndIabMarker.flatMap(Event(_))

      // TODO: DEBUGGING! Stop this when we enable the Ophan.send call
      ophanEvents.foreach(println)

      // TODO: Activate ophan send if everything looks ok
      // Ophan.send(ophanEvents)

    }
  }

}
