package com.gu.contentapi.models

import com.gu.contentapi.services.PodcastLookup
import net.openhft.hashing.LongHashFunction

case class Event(
  viewId: String,
  url: String,
  ipAddress: String,
  episodeId: String,
  podcastId: String,
  ua: String,
  platform: Option[String] = None
)

object Event {

  val hostUrl = "https://audio.guim.co.uk"

  def apply(fastlyLog: FastlyLog): Option[Event] = {

    val parsedUrl = okhttp3.HttpUrl.parse(hostUrl + fastlyLog.url)
    val path = parsedUrl.encodedPath()
    val absoluteUrlToFile = hostUrl + path

    PodcastLookup.getPodcastInfo(absoluteUrlToFile) map { info =>
      Event(
        viewId = LongHashFunction.xx_r39().hashChars(absoluteUrlToFile + fastlyLog.time + fastlyLog.ipAddress + fastlyLog.userAgent).toString,
        url = absoluteUrlToFile,
        ipAddress = fastlyLog.ipAddress,
        episodeId = info.episodeId,
        podcastId = info.podcastId,
        ua = fastlyLog.userAgent,
        platform = Option(parsedUrl.queryParameter("platform"))
      )
    }
  }

  def apply(acastLog: AcastLog): Option[Event] = {

    val filenameRaw = acastLog.query.split("&filename=").toList.lift(1)

    val withoutPrefix: String => String = { s => if (s.startsWith("The-Guardians-")) s.substring(14, s.length) else s }
    val withoutSuffix: String => String = { s => if (s.endsWith(".mp3")) s.substring(0, s.length - 4) else s }
    val reduce = withoutPrefix andThen withoutSuffix

    for {
      filename <- filenameRaw.map(reduce)
      info <- PodcastLookup.getPodcastInfoFromQuery(filename)
      absoluteUrlToFile <- info.absoluteUrl
    } yield {
      Event(
        viewId = acastLog.cloudfrontRequestId,
        url = absoluteUrlToFile,
        ipAddress = acastLog.ipAddress,
        episodeId = info.episodeId,
        podcastId = info.podcastId,
        ua = acastLog.userAgent,
        platform = None // TODO investigate if we could retrieved it from acastLog.query
      )
    }
  }
}