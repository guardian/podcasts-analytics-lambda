package com.gu.contentapi.services

import java.util.concurrent.TimeUnit

import com.gu.contentapi.client._
import com.gu.contentapi.client.model.v1.SearchResponse
import com.gu.contentapi.client.model.{ContentApiError, SearchQuery}

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}
import com.gu.contentapi.Config.capiKey
import okhttp3.OkHttpClient
import org.apache.logging.log4j.scala.Logging

import scala.collection.concurrent.TrieMap
import scala.util.control.NonFatal
import scala.util.{Failure, Success, Try}

object PodcastLookup extends Logging {

  import scala.concurrent.ExecutionContext.Implicits.global

  sealed trait ResponseFromCapiQuery
  case class SuccessfulQuery(searchResponse: SearchResponse) extends ResponseFromCapiQuery
  case class FailedQuery(errorMsg: String) extends ResponseFromCapiQuery

  val client = new GuardianContentClient(capiKey) {
    override def httpClientBuilder: OkHttpClient.Builder = {
      // CAPI requests sometimes fail due to java.net.SocketTimeoutException: timeout
      // It is suspected this is due to response not being read in (default CAPI-client read timeout of) 2s.
      // Since these requests are being executed in a lambda (and not in response to a client request),
      // we can safely increase the read timeout.
      super.httpClientBuilder.readTimeout(10, TimeUnit.SECONDS)
    }
  }

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

      //Try up to 3 times because we occasionally get capi timeouts
      val result: Try[Option[PodcastLookup.PodcastInfo]] = retry(3) {
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
      }

      result match {
        case Success(info) => info
        case Failure(NonFatal(e)) =>
          logger.error(s"CAPI request repeatedly failed: ${e.getMessage}", e)
          None
      }
    }
  }

  private def retry[T](retries: Int)(f: => T): Try[T] = {
    Try(f).recoverWith {
      case NonFatal(e) if retries > 1 =>
        logger.info(s"CAPI request failed with ${retries - 1} retries to go: ${e.getMessage}", e)
        retry(retries - 1)(f)
    }
  }
}
