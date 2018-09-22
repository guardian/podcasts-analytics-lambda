package com.gu.contentapi

object Config {

  val FastlyAudioLogsBucketName = "fastly-logs-audio"

  val capiKey = sys.env("CAPI_KEY")
  val capiUrl = sys.env.getOrElse("CAPI_URL", "https://content.guardianapis.com")
  val AcastAudioLogsBucketName = "acast-logs-audio"

  val AudioLogsBucketName = "gu-audio-logs"

}
