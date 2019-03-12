package com.gu.contentapi.utils

import com.amazonaws.services.s3.event.S3EventNotification.{ S3Entity, S3EventNotificationRecord }
import com.amazonaws.services.s3.model.S3Object
import com.amazonaws.util.IOUtils
import com.gu.contentapi.models.{ AcastLog, FastlyLog }
import com.gu.contentapi.services.S3
import com.gu.contentapi.Config
import java.io.InputStream
import scala.io.Source

sealed abstract class LogProcessor[A] {
  def process(objects: Iterable[S3EventNotificationRecord]): List[A]

  def download(obj: S3EventNotificationRecord): Option[InputStream] =
    S3.downloadReport(obj).map(_.getObjectContent)
}

object LogProcessor {
  lazy val acast = new AcastLogProcessor()
  lazy val fastly = new FastlyLogProcessor()

  final class FastlyLogProcessor private[LogProcessor] extends LogProcessor[FastlyLog] {
    final def process(objects: Iterable[S3EventNotificationRecord]) =
      normalProcess[FastlyLog](
        FastlyLog(_),
        r => FastlyLog.onlyDownloads(r) && FastlyLog.onlyGet(r),
        objects)
  }

  final class AcastLogProcessor private[LogProcessor] extends LogProcessor[AcastLog] {
    final def process(objects: Iterable[S3EventNotificationRecord]) =
      processAcast(normalProcess[AcastLog](
        AcastLog(_),
        r => AcastLog.onlyDownloads(r) && AcastLog.onlySuccessfulReponses(r) && AcastLog.onlyGet(r),
        objects))

    /**
     * Logs are aggregated according to three pieces of information: the user agent, the ip address
     * and the episode url: this is as close as we can get to a unique. For each group, we sum the
     * number of bytes being sent back and filter out entries that do not go beyond
     * `Config.minDownloadSize` bytes.
     */
    final def processAcast[A](logs: List[AcastLog]): List[AcastLog] =
      logs.foldLeft(Map.empty[(String, String, String), (List[AcastLog], Long)]) {
        case (entries, log) =>
          val size = log.bytes
          val key = (log.ipAddress, log.userAgent, log.url)
          entries + entries.get(key).foldLeft(key -> (List(log), size)) {
            case ((key, (log, size)), (logs, totalSize)) =>
              key -> (log ++ logs, size + totalSize)
          }
      }
        .values
        .collect {
          case (log :: Nil, _) => log
          case (log :: _, size) if size >= Config.minDownloadSize => log.copy(bytes = size)
        }
        .toList
  }

  def normalProcess[A](builder: String => Option[A], predicate: A => Boolean, objects: Iterable[S3EventNotificationRecord]): List[A] =
    objects.foldLeft(List.empty[A]) { (result, obj) =>
      S3.downloadReport(obj).map { report =>
        val logs = toDownloadLogs[A](predicate, builder)(report.getObjectContent)
        report.close()
        logs
      }.map(result ++ _).getOrElse(result)
    }

  def toDownloadLogs[A](predicate: A => Boolean, builder: String => Option[A])(input: InputStream): List[A] =
    Source
      .fromInputStream(input, "ISO-8859-1")
      .getLines
      .foldLeft(List.empty[A]) { (result, line) =>
        builder(line).filter(predicate).foldRight(result)(_ :: _)
      }

}