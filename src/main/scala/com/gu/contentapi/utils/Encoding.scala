package com.gu.contentapi.utils

import java.net.URLEncoder

object Encoding {

  /* Similar output to js encodeURIComponent - see https://stackoverflow.com/a/611117*/
  def encodeURIComponent(s: String) = {
    URLEncoder.encode(s, "UTF-8")
      .replaceAll("\\+", "%20")
      .replaceAll("\\%21", "!")
      .replaceAll("\\%27", "'")
      .replaceAll("\\%28", "(")
      .replaceAll("\\%29", ")")
      .replaceAll("\\%7E", "~")
  }
}

