package com.gu.contentapi.models

import scala.collection.immutable.ListMap

object AcastDecoder {
  val decodingMap = ListMap(
    "%3C" -> "<",
    "%3E" -> ">",
    "%2522" -> "\"",
    "%23" -> "#",
    "%7B" -> "{",
    "%7D" -> "}",
    "%7C" -> "|",
    "%255C" -> "\\",
    "%5E" -> "^",
    "%7E" -> "~",
    "%5B" -> "[",
    "%5D" -> "]",
    "%60" -> "`",
    "%27" -> "'",
    "%2520" -> " ",
    "%25" -> "%" /* should be the last to not override with %25XX */
  )

  def decode(encoded: String): String = {
    decodingMap.foldLeft(encoded) { case (cur, (from, to)) => cur.replaceAll(from, to) }
  }

}
