package com.gu.contentapi.utils

import purecsv.unsafe.CSVReader
import purecsv.unsafe.converter.RawFieldsConverter

import scala.util.{ Failure, Success, Try }
import scala.util.control.NonFatal
import org.apache.logging.log4j.scala.Logging

class CsvParser[T: RawFieldsConverter] extends Logging {
  private val reader = CSVReader[T]

  def parseLine(line: String): Option[T] = {

    /**
     * We use purecsv.unsafe here because despite returning a List[Try[T]], safe.readCSVFromString itself
     * can throw an exception.
     */
    val result: Try[Option[T]] = Try(reader.readCSVFromString(line).headOption)

    result match {
      case Success(Some(log)) => Some(log)
      case Success(None) =>
        logger.warn(s"Failed to parse line: '$line': no results returned by purecsv")
        None
      case Failure(e) if NonFatal(e) =>
        logger.warn(s"Failed to parse line: '$line': ${e.getMessage}", e)
        None
    }
  }
}
