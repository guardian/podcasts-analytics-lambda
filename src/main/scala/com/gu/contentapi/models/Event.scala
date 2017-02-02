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
        viewId = LongHashFunction.xx_r39().hashChars(absoluteUrlToFile + fastlyLog.time).toString,
        url = absoluteUrlToFile,
        ipAddress = fastlyLog.ipAddress,
        episodeId = info.episodeId,
        podcastId = info.podcastId,
        ua = fastlyLog.userAgent,
        platform = Option(parsedUrl.queryParameter("platform"))
      )
    }
  }

}