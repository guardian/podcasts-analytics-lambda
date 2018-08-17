package com.gu.contentapi.services

import com.gu.contentapi.client._
import com.gu.contentapi.client.model.v1.SearchResponse
import com.gu.contentapi.client.model.{ContentApiError, SearchQuery}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import com.gu.contentapi.Config.capiKey
import org.apache.logging.log4j.scala.Logging

import scala.collection.concurrent.TrieMap
import scala.util.control.NonFatal
import scala.util.{Failure, Success, Try}

object PodcastLookup extends Logging {

  import scala.concurrent.ExecutionContext.Implicits.global

  sealed trait ResponseFromCapiQuery
  case class SuccessfulQuery(searchResponse: SearchResponse) extends ResponseFromCapiQuery
  case class FailedQuery(errorMsg: String) extends ResponseFromCapiQuery

  val client = new GuardianContentClient(capiKey)

  case class PodcastInfo(
    episodeId: String,
    podcastId: String,
    absoluteUrl: Option[String] = None)

  val cache = TrieMap[String, PodcastInfo]()
  var cacheHits = 0
  var cacheMisses = 0

  private def makeCapiQuery(filePath: String): Future[ResponseFromCapiQuery] = {
    val query = SearchQuery()
      .filename(filePath)
      .showTags("all")

    client.getResponse(query) map { searchResponse: SearchResponse =>
      searchResponse.status match {
        case "ok" => SuccessfulQuery(searchResponse)
        case _ => FailedQuery(s"Failed query for filePath: $filePath")
      }
    } recover {
      case ContentApiError(status, msg, _) =>
        FailedQuery(s"Received unexpected response from CAPI: status $status with message $msg")
    }
  }

  def getPodcastInfo(filePath: String): Option[PodcastInfo] = {
    if (cache contains filePath) {
      cacheHits += 1
      cache.get(filePath)
    } else {
      cacheMisses += 1
      tryQuery(filePath) match {
        case Success(info) => info
        case Failure(NonFatal(e)) =>
          logger.error(s"CAPI request repeatedly failed: ${e.getMessage}", e)
          None
      }
    }
  }

  //Try up to 3 times because we occasionally get capi timeouts
  private def tryQuery(filePath: String, retries: Int = 3): Try[Option[PodcastInfo]] = {
    Try {
      Await.result(
        makeCapiQuery(filePath) map {
          case SuccessfulQuery(sr) =>
            for {
              result <- sr.results.headOption
              podcastName <- result.tags.find(_.podcast.isDefined).map(_.webUrl)
            } yield {
              val podcastInfo = PodcastInfo(result.webUrl, podcastName)
              cache.put(filePath, podcastInfo)
              podcastInfo
            }

          case FailedQuery(err) =>
            logger.error(s"Failed to get podcast info from capi for file '$filePath': $err")
            None
        }, 5.seconds)
    }.recoverWith {
      case NonFatal(e) if retries > 0 =>
        logger.info(s"CAPI request failed with ${retries-1} retries to go: ${e.getMessage}", e)
        tryQuery(filePath, retries - 1)
    }
  }
}
