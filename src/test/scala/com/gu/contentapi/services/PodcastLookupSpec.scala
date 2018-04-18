package com.gu.contentapi.services

import org.scalatest.{ FlatSpec, Matchers, OptionValues }

class PodcastLookupSpec extends FlatSpec with Matchers with OptionValues {

  it should "Retrieve podcast id and episode id from the filename" in {

    val info = PodcastLookup.getPodcastInfo("https://audio.guim.co.uk/2016/12/19-57926-FW-dec19-2016_mixdown.mp3")

    info.get.podcastId should be("https://www.theguardian.com/football/series/footballweekly")
    info.get.episodeId should be("https://www.theguardian.com/football/blog/audio/2016/dec/19/manchester-city-bounce-back-to-leave-wenger-fuming-football-weekly")
  }

  it should "Retrieve podcast id, episode id, and absolute url from the a query logs provided by acast" in {

    val info = PodcastLookup.getPodcastInfoFromQuery("Science-Weekly-Science-weekly-can-we-cure-Alzheimers-podcast")

    info.get.podcastId should be("https://www.theguardian.com/science/series/science")
    info.get.episodeId should be("https://www.theguardian.com/science/audio/2018/mar/30/the-trouble-with-science-science-weekly-podcast")
    info.get.absoluteUrl.get should be("https://audio.guim.co.uk/2018/03/30-33058-gnl.sci.180330.sf.the_natural_selection_of_bad_science.mp3")
  }

}