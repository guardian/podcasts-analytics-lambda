package com.gu.contentapi.services

import okhttp3.{ OkHttpClient, Request }
import com.gu.contentapi.models.Event
import com.gu.contentapi.utils.{ Encoding }

object Ophan {

  private val client = new OkHttpClient()
  private val OphanUrl = "https://ophan.theguardian.com/i.gif"

  //TODO handle time when the event has been received

  def send(events: Seq[Event]): Unit = {
    events foreach { e =>
      Thread.sleep(270000 / events.length) /* send events over a period of 4 minutes and 30 seconds */
      Ophan.send(e)
    }

    println(s"Events sent to Ophan: ${events.length}")
  }

  private def send(event: Event): Unit = {

    val Url = Encoding.encodeURIComponent(event.url)
    val IpAddress = Encoding.encodeURIComponent(event.ipAddress)
    val EpisodeId = Encoding.encodeURIComponent(event.episodeId)
    val PodcastId = Encoding.encodeURIComponent(event.podcastId)
    val UserAgent = Encoding.encodeURIComponent(event.ua)
    val Platform = event.platform.map(Encoding.encodeURIComponent)

    val url = s"$OphanUrl?url=$Url&viewId=${event.viewId}&isForwarded=true&ipAddress=$IpAddress&episodeId=$EpisodeId&podcastId=$PodcastId&ua=$UserAgent" + Platform.map(p => "&platform=" + p).getOrElse("")

    val request = new Request.Builder().url(url).build()

    client.newCall(request).execute()
  }

}