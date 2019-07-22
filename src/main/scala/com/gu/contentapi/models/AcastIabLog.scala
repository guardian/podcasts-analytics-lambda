package com.gu.contentapi.models

import com.gu.contentapi.utils.CsvParser

/*

timestamp
----------
The time. Stamped.

show_url
--------
Not a URL at all!

episode_url
-----------
Not a URL either. Just the title of the episode, lower-cased and hyphenated.
No idea why.

best_effort_user_id
-------------------
A hash based on the user's UA string and IP address. Here be dragons, especially
when more than one person has the same kind of device as others, and they're all
behind a common router

method
------
The http method. We're only interested in GETs but there may be others, e.g. HEADs.

status
------
The http result code. We're interested in 200s and 206s only

range_req
---------
An unreliable value that might contain a range with each part separated by a hyphen,
or half a range (a number and a hyphen then nothing else), or nothing at all

range_from
----------
If the range_req (above) had something in it, this should be whatever was before the
hyphen. Might also be zero.

range_to
--------
If the range_req (above) had something in it, this should be whatever was after the
hyphen.

bytes_delivered
---------------
How many bytes were sent to the agent in response to their request

stitch_size
-----------
Looks like this is the entire file size (I hope so because I'm making decisions based
on that)

ua_source
---------
That's a User Agent string to you and me

ip
--
A hash of the user's IP address, because GDPR

geo_city
--------
The city name the request originated in e.g. Gothenburg, Oldham

geo_country
-----------
The full country name of origin of the request, e.g. United Kingdom, Sweden

geo_country_iso
---------------
The universal short ID of the country of origin, e.g. GB, SE

geo_continent
-------------
The name of the continent or origin, e.g. Europe, Oceania

geo_is_eu
---------
true or false, depending on whether the country is in the EU

geo_postal
----------
The first part of the post code

geo_lat
-------
Latitude

geo_lon
-------
Longitude

geo_time_zone
-------------
The name of the time zone in effect in the identified country,
e.g. Europe/London, Australia/Sydney

geo_region
----------
The name of the territory within the defined country,
e.g. England, Scotland, New South Wales

geo_isp
-------
The name of the internet service provider, e.g. EE Mobile, SAKURA Internet

is_iab_valid
------------
Whether this request meets IAB guidelines for a 'complete' listen

filename
--------
The full path to the original media file (audio.guim)

*/

case class AcastIabLog(
  timestamp: String,
  showUrl: String, // meaningless
  episodeUrl: String, // meaningless
  userIdHash: String,
  httpMethod: String,
  httpStatus: String,
  rangeRequest: String,
  rangeFrom: String,
  rangeTo: String,
  bytesDelivered: String,
  stitchSize: String,
  userAgent: String,
  userIpHash: String,
  geoCity: String,
  geoCountry: String,
  geoCountryIsoCode: String,
  geoContinent: String,
  geoIsEu: String,
  geoPostalCode: String,
  geoLatitude: String,
  geoLongitude: String,
  geoTimeZone: String,
  geoRegion: String,
  geoIsp: String,
  isIabValid: String,
  sourceFileName: String) {
  def toCsv: String = {
    this.productIterator.map {
      case s: String => '"' + s + '"'
    }.mkString(",")
  }
}

object AcastIabLog {

  private val parser = new CsvParser[AcastIabLog]

  val validRangeRequestsFilter: (AcastIabLog => Boolean) = { log =>
    val allowedRangePattern = """^0-(?!1$)""".r // this is duplicated from AcastLog - refactor out maybe
    allowedRangePattern.findFirstIn(log.rangeRequest).nonEmpty || (log.rangeFrom.toInt == 0 && log.rangeTo.toInt > 1)
  }

  val validHttpMethodAndStatus: (AcastIabLog => Boolean) = { logEntry =>
    logEntry.httpMethod == "GET" &&
      (logEntry.httpStatus == "200" || logEntry.httpStatus == "206")
  }

  def userRequests(logs: Seq[AcastIabLog], userIdHash: String): Seq[AcastIabLog] = {
    logs.filter(_.userIdHash == userIdHash)
  }

  def apply(line: String): Option[AcastIabLog] =
    parser.parseLine(line).map(log =>
      log.copy(userAgent = AcastDecoder.decode(log.userAgent), sourceFileName = AcastDecoder.decode(log.sourceFileName)))

}

