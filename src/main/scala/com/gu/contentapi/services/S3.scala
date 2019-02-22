package com.gu.contentapi.services

import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.{ AmazonS3, AmazonS3ClientBuilder }
import com.amazonaws.services.s3.event.S3EventNotification.{ S3Entity, S3EventNotificationRecord }
import com.amazonaws.services.s3.model.S3Object
import org.apache.logging.log4j.scala.Logging

import scala.util.{ Failure, Success, Try }

object S3 extends Logging {

  val client: AmazonS3 = AmazonS3ClientBuilder.standard().withRegion(Regions.EU_WEST_1).build()

  def downloadReport(notif: S3EventNotificationRecord): Option[S3Object] = {
    val entity = notif.getS3
    val reportName = entity.getBucket.getName

    Try(client.getObject(
      reportName,
      notif.getS3.getObject.getKey.replaceAll("%3A", ":")
    // ^ looks like the json object is not decoded properly by the SDK
    )) match {
      case Success(report) => {
        logger.info(s"Downloaded report $reportName")
        Some(report)
      }
      case Failure(e) => {
        logger.warn(s"Failed to download $reportName - exception thrown $e")
        None
      }
    }
  }

}
