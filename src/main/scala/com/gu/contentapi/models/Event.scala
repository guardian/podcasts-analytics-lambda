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
  platform: Option[String] = None)

object Event {

  val hostUrl = "https://audio.guim.co.uk"

  def apply(fastlyLog: FastlyLog): Option[Event] = {

    val parsedUrl = okhttp3.HttpUrl.parse(hostUrl + fastlyLog.url)
    val path = parsedUrl.encodedPath()
    val absoluteUrlToFile = hostUrl + path

    for {
      rawPath <- fastlyLog.url.split("""\?""").headOption //The CAPI client takes care of url encoding
      info <- PodcastLookup.getPodcastInfo(hostUrl + rawPath)
    } yield Event(
      viewId = LongHashFunction.xx_r39().hashChars(absoluteUrlToFile + fastlyLog.time + fastlyLog.ipAddress + fastlyLog.userAgent).toString,
      url = absoluteUrlToFile,
      ipAddress = fastlyLog.ipAddress,
      episodeId = info.episodeId,
      podcastId = info.podcastId,
      ua = fastlyLog.userAgent,
      platform = Option(parsedUrl.queryParameter("platform")))
  }
}
