package com.gu.contentapi.utils

import purecsv.unsafe.CSVReader
import purecsv.unsafe.converter.RawFieldsConverter

import scala.util.{ Failure, Success, Try }
import scala.util.control.NonFatal
import org.apache.logging.log4j.scala.Logging
import purecsv.config.{ Headers, Trimming }

import scala.reflect.ClassTag

class CsvParser[T: RawFieldsConverter](implicit classTag: ClassTag[T]) extends Logging {
  private val reader = CSVReader[T]

  def parseLine(line: String): Option[T] = {

    /**
     * We use purecsv.unsafe here because despite returning a List[Try[T]], safe.readCSVFromString itself
     * can throw an exception.
     */
    val result: Try[Option[T]] = Try(reader.readCSVFromString(line, trimming = Trimming.TrimAll, headers = Headers.None).headOption)

    result match {
      case Success(Some(log)) => Some(log)
      case Success(None) =>
        logger.warn(s"Failed to parse line: '$line': no results returned by purecsv")
        None
      case Failure(e) if NonFatal(e) =>
        logger.warn(s"Failed to parse line: '$line': ${e.getMessage}", e)
        None
      case Failure(err) => //if this _is_ a fatal exception, throw it and let the Lambda runtime error out. This should show up in metrics.
        throw err
    }
  }
}
