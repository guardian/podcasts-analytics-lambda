package com.gu.contentapi.services

import java.nio.charset.Charset
import java.nio.file.{ Files, Path }
import scala.io.{ Codec, Source }
import scala.util.Try

object Loader {
  //Loads the tempfile into a set of lines and deletes it from the disk
  def linesFromFile(path: Path): Try[Seq[String]] = {
    implicit val codec: Codec = Codec.charset2codec(Charset.forName("ISO-8859-1"))
    val s = Try { Source.fromFile(path.toFile) }
    val content = s.flatMap(src => Try { src.getLines().toSeq })
    s.map(_.close())
    Files.deleteIfExists(path)
    content
  }
}
