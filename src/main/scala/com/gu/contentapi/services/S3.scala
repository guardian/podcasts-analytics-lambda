package com.gu.contentapi.services

import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.{ AmazonS3, AmazonS3ClientBuilder }
import com.amazonaws.services.s3.model.S3Object
import org.apache.logging.log4j.scala.Logging

import scala.util.{ Failure, Success, Try }

object S3 extends Logging {

  private val retryDelayMs = 100

  val client: AmazonS3 = AmazonS3ClientBuilder.standard().withRegion(Regions.EU_WEST_1).build()

  def downloadReport(bucketName: String, reportName: String, retries: Int = 3): Option[S3Object] = {
    Try(client.getObject(bucketName, reportName + "XX")) match {
      case Success(report) => {
        logger.info(s"Downloaded report $reportName")
        Some(report)
      }
      case Failure(e) => {
        if (retries > 0) {
          logger.info(s"Failed to download $reportName - remaining retries: $retries - exception thrown $e")
          Thread.sleep(retryDelayMs)
          downloadReport(bucketName, reportName, retries - 1)
        } else {
          logger.warn(s"Failed to download $reportName - no retries left - exception thrown $e")
          None
        }
      }
    }
  }

}
