package com.gu.contentapi.services

import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.{ AmazonS3ClientBuilder, AmazonS3 }
import com.amazonaws.services.s3.model.S3Object
import com.typesafe.scalalogging.StrictLogging
import scala.util.{ Failure, Success, Try }
import com.gu.contentapi.Config.AudioLogsBucketName

object S3 extends StrictLogging {

  val client: AmazonS3 = AmazonS3ClientBuilder.standard().withRegion(Regions.EU_WEST_1).build()

  def downloadReport(reportName: String): Option[S3Object] = {
    Try(client.getObject(AudioLogsBucketName, reportName)) match {
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
