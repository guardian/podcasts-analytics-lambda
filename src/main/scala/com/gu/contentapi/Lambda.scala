package com.gu.contentapi

import com.amazonaws.services.lambda.runtime.events.S3Event
import com.amazonaws.services.lambda.runtime.{ Context, RequestHandler }
import scala.collection.JavaConverters._
import com.gu.contentapi.Config.AudioLogsBucketName
import com.gu.contentapi.models.{ Event, FastlyLog }
import com.gu.contentapi.services.{ PodcastLookup, S3 }
import com.gu.contentapi.utils.{ Encoding }
import com.typesafe.scalalogging.StrictLogging
import okhttp3.{ OkHttpClient, Request }
import com.amazonaws.util.IOUtils
import scala.io.Source

class Lambda extends RequestHandler[S3Event, Unit] with StrictLogging {

  override def handleRequest(event: S3Event, context: Context): Unit = {

    val logObjects = event.getRecords.asScala
      .filter(_.getS3.getBucket.getName == AudioLogsBucketName)
      .flatMap(reportObject => S3.downloadReport(reportObject.getS3.getObject.getKey.replaceAll("%3A", ":"))) // looks like the json object is not decoded properly by the SKD
      .toList

    logObjects foreach { logObj =>

      val allFastlyLogs: Seq[FastlyLog] = Source.fromBytes(IOUtils.toByteArray(logObj.getObjectContent))("ISO-8859-1").getLines().toSeq flatMap { line =>
        FastlyLog(line)
      }

      println(s"DEBUG: I got ${allFastlyLogs.length} to process from logfile")

      val downloadsLogs = allFastlyLogs.filter(FastlyLog.onlyDownloads)

      val events: Seq[Event] = downloadsLogs.flatMap(Event(_))

      events foreach { e =>
        Thread.sleep(270000 / events.length) // send events over a period of 4 minutes and 30 seconds
        Ophan.send(e)
      }

      println("Done :) -- FYI:")
      println(s"Cache hits: ${PodcastLookup.cacheHits}")
      println(s"Cache misses: ${PodcastLookup.cacheMisses}")
      println(s"Events sent to Ophan: ${events.length}")
    }
  }
}

object Ophan {

  private val client = new OkHttpClient()
  private val OphanUrl = "https://ophan.theguardian.com/i.gif"

  def send(event: Event): Unit = {
    val Url = Encoding.encodeURIComponent(event.url)
    val IpAddress = Encoding.encodeURIComponent(event.ipAddress)
    val EpisodeId = Encoding.encodeURIComponent(event.episodeId)
    val PodcastId = Encoding.encodeURIComponent(event.podcastId)
    val UserAgent = Encoding.encodeURIComponent(event.ua)
    val platform = event.platform.map(Encoding.encodeURIComponent)

    val url = s"$OphanUrl?url=$Url&viewId=${event.viewId}&isForwarded=true&ipAddress=$IpAddress&episodeId=$EpisodeId&podcastId=$PodcastId&ua=$UserAgent" + platform.map(p => "&platform=" + p).getOrElse("")

    val request = new Request.Builder().url(url).build()

    client.newCall(request).execute()
  }

}
