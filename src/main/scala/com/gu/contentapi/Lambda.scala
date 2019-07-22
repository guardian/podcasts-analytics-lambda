package com.gu.contentapi

import java.io.{ File, FileOutputStream }

import com.amazonaws.services.lambda.runtime.events.S3Event
import com.amazonaws.services.lambda.runtime.{ Context, RequestHandler }
import com.amazonaws.services.s3.event.S3EventNotification.S3Entity
import com.amazonaws.services.s3.model.S3Object

import scala.collection.JavaConverters._
import com.gu.contentapi.models.{ AcastIabLog, AcastLog, Event, FastlyLog }
import com.gu.contentapi.services.{ Ophan, PodcastLookup, S3 }
import com.amazonaws.util.IOUtils

import scala.io.Source
import org.apache.logging.log4j.scala.Logging

case class PartitionedLogs(http200: Seq[AcastIabLog], http206: Seq[AcastIabLog])
case class UserFileRequests(userIdHash: String, fileRequests: Map[String, Seq[AcastIabLog]])
case class UserLogs(downloads: Seq[UserFileRequests], interactives: Seq[UserFileRequests])

class Lambda extends RequestHandler[S3Event, Unit] with Logging {

  override def handleRequest(event: S3Event, context: Context): Unit = {

    // TODO: reinstate when done testing
    //    val fastlyReportObjects = event.getRecords.asScala
    //      .filter(rec => isLogType(Config.FastlyAudioLogsBucketName, rec.getS3))
    //      .flatMap(rec => S3.downloadReport(rec.getS3.getBucket.getName, rec.getS3.getObject.getKey.replaceAll("%3A", ":"))) // looks like the json object is not decoded properly by the SKD
    //      .toList

    // TODO: *maybe* reinstate this when done testing, or delete altogether
    //    val acastReportObjects = event.getRecords.asScala
    //      .filter(rec => isLogType(Config.AcastAudioLogsBucketName, rec.getS3))
    //      .flatMap(rec => S3.downloadReport(rec.getS3.getBucket.getName, rec.getS3.getObject.getKey.replaceAll("%3A", ":"))) // looks like the json object is not decoded properly by the SKD
    //      .toList

    val acastIabReportObjects = event.getRecords.asScala
      .filter(rec => isLogType(Config.AcastIabAudioLogsBucketName, rec.getS3))
      .filter(rec => isCsvFile(rec.getS3))
      .flatMap(rec => S3.downloadReport(rec.getS3.getBucket.getName, rec.getS3.getObject.getKey.replaceAll("%3A", ":"))) // looks like the json object is not decoded properly by the SKD
      .toList

    // TODO: reinstate after testing
    //    handleReportsFromFastly(fastlyReportObjects)

    // TODO: reinstate *maybe*, or delete altogether
    //    handleReportsFromAcast(acastReportObjects)

    handleIabReportsFromAcast(acastIabReportObjects)

    logger.debug(s"Cache hits: ${PodcastLookup.cacheHits}")
    logger.debug(s"Cache misses: ${PodcastLookup.cacheMisses}")

  }

  private def isLogType(typeName: String, s3Entity: S3Entity): Boolean =
    s3Entity.getBucket.getName == typeName ||
      s3Entity.getObject.getKey.startsWith(typeName)

  private def isCsvFile(s3Entity: S3Entity): Boolean =
    s3Entity.getObject.getKey.endsWith(".csv")

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

    objects foreach { obj =>

      // get the log entries from the file that are 200 or 206 responses and have a valid range (if any)
      val iabValidLogs = extractIABValidLogs(Source.fromBytes(IOUtils.toByteArray(obj.getObjectContent))("ISO-8859-1").getLines().toSeq)

      // TODO:
      // while we're debugging, we're going to write our condensed processed data to a
      // temporary file so we can check it manually (it's still a CSV of sorts)
      // we'll remove all this later, leaving just the Ophan event creation in place
      val outputFileName = obj.getKey().replaceFirst(".csv", "")
      val outputFile = File.createTempFile(outputFileName, ".tmp", new File("/tmp"))
      val outputStream = new FileOutputStream(outputFile)
      iabValidLogs.foreach { log =>
        outputStream.write(log.toCsv.getBytes)
        outputStream.write("\n".getBytes)
      }
      outputStream.flush()
      outputStream.close()
      S3.client.putObject(obj.getBucketName(), outputFileName + ".condensed", outputFile)
      outputFile.delete()

      // TODO: Begin sending events to ophan when everything looks ok
      // val ophanEvents = distinctUserFileRequestsWithAggregatedBytesAndIabMarker.flatMap(Event(_))
      // Ophan.send(ophanEvents)

    }
  }

  private def ninetyPercentDelivered(row: AcastIabLog): Boolean = {
    val thresholdPercent = 90
    val bytesDelivered = (0 + row.bytesDelivered).toInt
    val stitchSize = (0 + row.stitchSize).toInt
    (bytesDelivered / stitchSize) * 100 >= thresholdPercent
  }

  private def reachedEndOfStream(row: AcastIabLog): Boolean = {
    val stitchSize: Int = (0 + row.stitchSize).toInt
    val rangeFrom: Int = (0 + row.rangeFrom).toInt
    val rangeTo: Int = (0 + row.rangeTo).toInt
    val bytesDelivered: Int = (0 + row.bytesDelivered).toInt
    val threshold: Int = 2048 // arbitrary number - 2Kb from end of file

    // return true if bytes were delivered
    // AND rangeFrom + bytesDelivered is within "threshold" bytes of the total stitch(file)Size
    // OR rangeFrom + bytesDelivered is >= stitch(file)Size)
    bytesDelivered > 0 &&
      (rangeFrom + bytesDelivered >= stitchSize - threshold) || (rangeFrom + bytesDelivered >= stitchSize)
  }

  private def extractIABValidLogs(logs: Seq[String]): Seq[AcastIabLog] = {
    val validLogs = logs.flatMap { line =>
      AcastIabLog(line)
    }.filter(AcastIabLog.validHttpMethodAndStatus).partition(_.httpStatus == "200")
    val validDownloadListens = validLogs._1.filter(ninetyPercentDelivered).map(_.copy(isIabValid = "true"))
    val validInteractiveListens = validLogs._2.filter(reachedEndOfStream).map(_.copy(isIabValid = "true"))

    validDownloadListens ++ validInteractiveListens

  }

}
