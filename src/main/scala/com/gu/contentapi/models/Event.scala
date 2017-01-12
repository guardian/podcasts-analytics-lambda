package com.gu.contentapi.models

import com.gu.contentapi.services.PodcastLookup
import net.openhft.hashing.LongHashFunction

case class Event(
  viewId: String,
  url: String,
  ipAddress: String,
  episodeId: String,
  podcastId: String
)

object Event {

  def apply(fastlyLog: FastlyLog): Option[Event] = {
    val fullPathToFile = s"https://audio.guim.co.uk${fastlyLog.url}"

    PodcastLookup.getPodcastInfo(fullPathToFile) map { info =>
      Event(
        viewId = LongHashFunction.xx_r39().hashChars(fullPathToFile + fastlyLog.time).toString,
        url = fullPathToFile,
        ipAddress = fastlyLog.ipAddress,
        episodeId = info.episodeId,
        podcastId = info.podcastId
      )
    }
  }

}