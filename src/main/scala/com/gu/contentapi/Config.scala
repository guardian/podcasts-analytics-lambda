package com.gu.contentapi

object Config {

  val FastlyAudioLogsBucketName = "fastly-logs-audio"

  val capiKey = sys.env("CAPI_KEY")
  val AcastAudioLogsBucketName = "acast-logs-audio"
  val AcastIabAudioLogsBucketName = "acast-iab-logs-audio"

  val AudioLogsBucketName = "gu-audio-logs"

}
