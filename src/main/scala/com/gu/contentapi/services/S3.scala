package com.gu.contentapi.services

import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.{ AmazonS3, AmazonS3ClientBuilder }
import com.amazonaws.services.s3.model.S3Object
import org.apache.logging.log4j.scala.Logging

import scala.util.{ Failure, Success, Try }

object S3 extends Logging {

  val client: AmazonS3 = AmazonS3ClientBuilder.standard().withRegion(Regions.EU_WEST_1).build()

  def downloadReport(bucketName: String, reportName: String): Option[S3Object] = {
    Try(client.getObject(bucketName, reportName)) match {
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
