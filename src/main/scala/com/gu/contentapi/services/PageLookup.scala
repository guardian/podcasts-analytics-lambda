package com.gu.contentapi.services

import com.gu.contentapi.client._
import com.gu.contentapi.client.model.v1.SearchResponse
import com.gu.contentapi.client.model.SearchQuery
import com.typesafe.scalalogging.StrictLogging
import scala.concurrent.duration._
import scala.concurrent.{ Await, Future }
import com.gu.contentapi.Config.capiKey
import scala.collection.concurrent.TrieMap

object PageLookup extends StrictLogging {

  import scala.concurrent.ExecutionContext.Implicits.global

  sealed trait ResponseFromCapiQuery
  case class SuccessfulQuery(searchResponse: SearchResponse) extends ResponseFromCapiQuery
  case class FailedQuery(errorMsg: String) extends ResponseFromCapiQuery

  val client = new GuardianContentClient(capiKey)

  val cache = TrieMap[String, String]()
  var cacheHits = 0
  var cacheMisses = 0

  private def makeCapiQuery(filePath: String): Future[ResponseFromCapiQuery] = {
    val query = SearchQuery().filename(filePath)

    client.getResponse(query) map { searchResponse: SearchResponse =>
      searchResponse.status match {
        case "ok" => SuccessfulQuery(searchResponse)
        case _ => FailedQuery(s"Failed query for filePath: $filePath")
      }
    } recover {
      case GuardianContentApiError(status, msg, _) =>
        FailedQuery(s"Received unexpected response from CAPI: status $status with message $msg")
    }
  }

  def getPagePath(filePath: String): Option[String] = {
    if (cache contains filePath) {
      cacheHits += 1
      cache.get(filePath)
    } else {
      cacheMisses += 1
      Await.result(
        makeCapiQuery(filePath) map {
          case SuccessfulQuery(sr) => {
            sr.results.headOption foreach { content => cache += (filePath -> content.webUrl) }
            sr.results.headOption map (_.webUrl)
          }
          case FailedQuery(err) => Some(err)
        }, 2.seconds
      )
    }
  }

}
