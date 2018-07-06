package com.gu.contentapi.services

import org.scalatest.{ FlatSpec, Matchers, OptionValues }

class PodcastLookupSpec extends FlatSpec with Matchers with OptionValues {

  it should "Retrieve podcast id and episode id from the filename" in {

    val info = PodcastLookup.getPodcastInfo("https://audio.guim.co.uk/2016/12/19-57926-FW-dec19-2016_mixdown.mp3")

    info.get.podcastId should be("https://www.theguardian.com/football/series/footballweekly")
    info.get.episodeId should be("https://www.theguardian.com/football/blog/audio/2016/dec/19/manchester-city-bounce-back-to-leave-wenger-fuming-football-weekly")
  }

}