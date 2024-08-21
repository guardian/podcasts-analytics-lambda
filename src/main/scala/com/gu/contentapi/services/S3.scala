package com.gu.contentapi.services

import org.apache.logging.log4j.scala.Logging
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.transfer.s3.S3TransferManager
import software.amazon.awssdk.transfer.s3.model.DownloadFileRequest
import java.nio.file.{ Files, Path }
import scala.jdk.FutureConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object S3 extends Logging {

  private val retryDelayMs = 100

  private val client: S3AsyncClient = S3AsyncClient.builder().region(Region.EU_WEST_1).multipartEnabled(true).build()
  private val transferManager: S3TransferManager = S3TransferManager.builder().s3Client(client).build()

  private def downloadToTemp(bucketName: String, reportName: String) = {
    val tempFile = Files.createTempFile("fastly", ".txt")
    val req = DownloadFileRequest.builder()
      .getObjectRequest(GetObjectRequest.builder().bucket(bucketName).key(reportName).build())
      .destination(tempFile)
      .build()

    val f = transferManager.downloadFile(req)
    f.completionFuture().asScala.map(_ => tempFile)
  }

  def downloadReport(bucketName: String, reportName: String, retries: Int = 3): Future[Option[Path]] = {
    (for {
      file <- downloadToTemp(bucketName, reportName)
    } yield Some(file)).recoverWith({
      case e: Throwable =>
        if (retries > 0) {
          logger.info(s"Failed to download $reportName - remaining retries: $retries - exception thrown $e")
          Thread.sleep(retryDelayMs)
          downloadReport(bucketName, reportName, retries - 1)
        } else {
          logger.warn(s"Failed to download $reportName - no retries left - exception thrown $e")
          Future(None)
        }
    })
  }

}
