package com.gu.contentapi.utils

import com.amazonaws.services.s3.event.S3EventNotification.{ S3Entity, S3EventNotificationRecord }
import com.amazonaws.services.s3.model.S3Object
import com.amazonaws.util.IOUtils
import com.gu.contentapi.models.{ AcastLog, FastlyLog }
import com.gu.contentapi.services.S3
import com.gu.contentapi.Config
import scala.io.Source

sealed abstract class LogProcessor[A] {
  def process(objects: Iterable[S3EventNotificationRecord]): List[A]
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
    private final def processAcast[A](logs: List[AcastLog]): List[AcastLog] =
      logs.foldLeft(Map.empty[(String, String, String), (AcastLog, Long)]) {
        case (entries, log) =>
          val size = log.bytes
          val key = (log.ipAddress, log.userAgent, log.url)
          entries + entries.get(key).foldLeft(key -> (log, size)) {
            case ((key, (log, size)), entry) =>
              key -> (log, size + entry._2)
          }
      }
        .values
        .filter(_._2 >= Config.minDownloadSize)
        .map(_._1)
        .toList
  }

  def normalProcess[A](builder: String => Option[A], predicate: A => Boolean, objects: Iterable[S3EventNotificationRecord]): List[A] =
    objects.toList
      .flatMap(S3.downloadReport(_).map(toDownloadLogs[A](predicate, builder)).getOrElse(Nil))

  def toDownloadLogs[A](predicate: A => Boolean, builder: String => Option[A])(obj: S3Object): List[A] =
    Source
      .fromBytes(IOUtils.toByteArray(obj.getObjectContent))("ISO-8859-1")
      .getLines
      .toList
      .flatMap(l => builder(l).map(List(_)).getOrElse(Nil))
      .filter(predicate)

}