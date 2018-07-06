package com.gu.contentapi.utils

import purecsv.safe.CSVReader
import purecsv.safe.converter.RawFieldsConverter

import scala.util.{ Failure, Success, Try }
import scala.util.control.NonFatal
import org.apache.logging.log4j.scala.Logging

class CsvParser[T: RawFieldsConverter] extends Logging {
  private val reader = CSVReader[T]

  def parseLine(line: String): Option[T] = {
    //despite returning a List[Try[T]], readCSVFromString itself can throw an exception, so catch that as well
    val result: Try[T] = for {
      parsed: List[Try[T]] <- Try(reader.readCSVFromString(line))
      log <- parsed.headOption.getOrElse(Failure(new Exception("Empty list of logs returned by parser")))
    } yield log

    result match {
      case Success(log) => Some(log)
      case Failure(e) if NonFatal(e) =>
        logger.warn(s"Failed to parse line: '$line': ${e.getMessage}", e)
        None
    }
  }
}
