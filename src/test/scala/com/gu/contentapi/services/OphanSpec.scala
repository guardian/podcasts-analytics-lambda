package com.gu.contentapi.services

import com.gu.contentapi.models.{ Event }
import org.scalatest.{ FlatSpec, Matchers, OptionValues }

class OphanSpec extends FlatSpec with Matchers with OptionValues {

  it should "send a single podcast to Ophan" in {

    val ev = Event(
      viewId = "-5103960900567454554",
      url = "https://audio.guim.co.uk/2016/11/10-58860-FW-10nov-2016_mixdown.mp3",
      ipAddress = "66.87.114.159",
      episodeId = "https://www.theguardian.com/football/audio/2016/nov/10/gordon-strachans-last-stand-with-scotland-football-weekly-extra",
      podcastId = "https://www.theguardian.com/football/series/footballweekly",
      ua = "Samsung SM-G900P stagefright/Beyonce/1.1.9 (Linux;Android 6.0.1)")
    //    Ophan.send(ev)
  }

}