package com.gu.contentapi

import com.amazonaws.services.lambda.runtime.events.S3Event
import com.amazonaws.services.lambda.runtime.{ Context, RequestHandler }
import scala.collection.JavaConverters._
import com.gu.contentapi.Config.AudioLogsBucketName
import com.gu.contentapi.models.FastlyLog
import com.gu.contentapi.services.S3
import com.gu.contentapi.utils.WriteToFile
import com.typesafe.scalalogging.StrictLogging
import scala.io.Source

class Lambda extends RequestHandler[S3Event, Unit] with StrictLogging {

  override def handleRequest(event: S3Event, context: Context): Unit = {

    val logObjects = event.getRecords.asScala
      .filter(_.getS3.getBucket.getName == AudioLogsBucketName)
      .flatMap(reportObject => S3.downloadReport(reportObject.getS3.getObject.getKey.replaceAll("%3A", ":"))) // looks like the json object is not decoded properly by the SKD
      .toList

    logObjects foreach { logObj =>
      WriteToFile.fromLogObject(logObj, s"/tmp/${logObj.getKey}")

      val allFastlyLogs: Seq[FastlyLog] = Source.fromFile(s"/tmp/${logObj.getKey}").getLines().toSeq flatMap { line =>
        FastlyLog(line)
      }

      logger.info(s"DEBUG: I got ${allFastlyLogs.length} to process from logfile ${logObj.getKey}")

      // TODO: foreach fastlylog, fill in the podcast page url and send the result to Ophan
    }

  }

}
