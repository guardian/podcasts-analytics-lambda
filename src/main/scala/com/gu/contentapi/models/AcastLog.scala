package com.gu.contentapi.models

import com.gu.contentapi.utils.CsvParser

import purecsv.safe._

import scala.util.Success
import scala.collection.immutable.ListMap

/*

timestamp
----------


sc-bytes
----------
The total number of bytes that CloudFront served to the viewer in response to the request, including headers

byte-range
-----------


c-ip
----------
The IP address of the viewer that made the request, for example, 192.0.2.183 or 2001:0db8:85a3:0000:0000:8a2e:0370:7334. If the viewer used an HTTP proxy or a load balancer to send the request, the value of c-ip is the IP address of the proxy or load balancer

cs-method
-----------
The HTTP access method: DELETE, GET, HEAD, OPTIONS, PATCH, POST, or PUT

cs-uri-stem
-----------
The portion of the URI that identifies the path and object, for example, /images/daily-ad.jpg.

sc-status
----------
One of the following values:

 * An HTTP status code (for example, 200). For a list of HTTP status codes, see RFC 2616, Hypertext Transfer Protocol—HTTP 1.1, section 10, Status Code Definitions. For more information, see How CloudFront Processes and Caches HTTP 4xx and 5xx Status Codes from Your Origin.
 * 000, which indicates that the viewer closed the connection (for example, closed the browser tab) before CloudFront could respond to a request.

cs(Referer)
-----------
The name of the domain that originated the request. Common referrers include search engines, other websites that link directly to your objects, and your own website.

cs(User-Agent)
--------------

cs-uri-query
-------------
The query string portion of the URI, if any. When a URI doesn't contain a query string, the value of cs-uri-query is a hyphen (-).

x-host-header
-------------
The value that the viewer included in the Host header for this request. This is the domain name in the request:

If you're using the CloudFront domain name in your object URLs, such as http://d111111abcdef8.cloudfront.net/logo.png, the x-host-header field contains that domain name.
If you're using alternate domain names in your object URLs, such as http://example.com/logo.png, the x-host-header field contains the alternate domain name, such as example.com. To use alternate domain names, you must add them to your distribution. For more information, see Using Alternate Domain Names (CNAMEs).
If you're using alternate domain names, see cs(Host) in field 7 for the domain name that is associated with your distribution.

x-forwarded-for
---------------
If the viewer used an HTTP proxy or a load balancer to send the request, the value of c-ip in field 5 is the IP address of the proxy or load balancer. In that case, x-forwarded-for is the IP address of the viewer that originated the request.
If the viewer did not use an HTTP proxy or a load balancer, the value of x-forwarded-for is a hyphen (-).

x-edge-result-type
------------------

How CloudFront classifies the response after the last byte left the edge location. In some cases, the result type can change between the time that CloudFront is ready to send the response and the time that CloudFront has finished sending the response. For example, in HTTP streaming, suppose CloudFront finds a segment in the edge cache. The value of x-edge-response-result-type, the result type immediately before CloudFront begins to respond to the request, is Hit. However, if the user closes the viewer before CloudFront has delivered the entire segment, the final result type—the value of x-edge-result-type—changes to Error.

Possible values include:

 * Hit – CloudFront served the object to the viewer from the edge cache.
   For information about a situation in which CloudFront classifies the result type as Hit even though the response from the origin contains a Cache-Control: no-cache header, see Simultaneous Requests for the Same Object (Traffic Spikes).
 * RefreshHit – CloudFront found the object in the edge cache but it had expired, so CloudFront contacted the origin to determine whether the cache has the latest version of the object and, if not, to get the latest version.
 * Miss – The request could not be satisfied by an object in the edge cache, so CloudFront forwarded the request to the origin server and returned the result to the viewer.
 * LimitExceeded – The request was denied because a CloudFront limit was exceeded.
 * CapacityExceeded – CloudFront returned an HTTP 503 status code (Service Unavailable) because the CloudFront edge server was temporarily unable to respond to requests.
 * Error – Typically, this means the request resulted in a client error (sc-status is 4xx) or a server error (sc-status is 5xx).
 * Redirect – CloudFront redirects from HTTP to HTTPS.
   If sc-status is 403 and you configured CloudFront to restrict the geographic distribution of your content, the request might have come from a restricted location. For more information about geo restriction, see Restricting the Geographic Distribution of Your Content.
   If the value of x-edge-result-type is Error and the value of x-edge-response-result-type is not Error, the client disconnected before finishing the download.


x-edge-response-result-type
----------------------------

How CloudFront classified the response just before returning the response to the viewer. See also x-edge-result-type in field 14.

Possible values include:

 * Hit – CloudFront served the object to the viewer from the edge cache.
 * RefreshHit – CloudFront found the object in the edge cache but it had expired, so CloudFront contacted the origin to verify that the cache has the latest version of the object.
 * Miss – The request could not be satisfied by an object in the edge cache, so CloudFront forwarded the request to the origin server and returned the result to the viewer.
 * LimitExceeded – The request was denied because a CloudFront limit was exceeded.
 * CapacityExceeded – CloudFront returned a 503 error because the edge location didn't have enough capacity at the time of the request to serve the object.
 * Error – Typically, this means the request resulted in a client error (sc-status is 4xx) or a server error (sc-status is 5xx).
 * Redirect – CloudFront redirects from HTTP to HTTPS.

If sc-status is 403 and you configured CloudFront to restrict the geographic distribution of your content, the request might have come from a restricted location. For more information about geo restriction, see Restricting the Geographic Distribution of Your Content.
If the value of x-edge-result-type is Error and the value of x-edge-response-result-type is not Error, the client disconnected before finishing the download.


x-edge-request-id
-----------------
An encrypted string that uniquely identifies a request.

----------------
| Other fields |
----------------

city
country
country code
region code
continent code
dma code
postal_code
longitude
latitude

*/

case class AcastLog(
  timestamp: String,
  bytes: String,
  range: String,
  ipAddress: String,
  request: String,
  url: String, /* relative URL */
  status: String,
  referrer: String,
  userAgent: String,
  query: String,
  host: String,
  forwardedFor: String,
  cloudfrontResultType: String,
  cloudfrontResponseResultType: String,
  cloudfrontRequestId: String,
  city: String,
  country: String,
  countryCode: String,
  region: String,
  continentCode: String,
  dmaCode: String,
  postalCode: String,
  longitude: String,
  latitude: String,
  filename: String)

object AcastLog {

  private val parser = new CsvParser[AcastLog]

  def apply(line: String): Option[AcastLog] =
    parser.parseLine(line).map(log =>
      log.copy(userAgent = AcastDecoder.decode(log.userAgent), filename = AcastDecoder.decode(log.filename)))

  /* filter out partial content requests unless the byte-range starts from 0 and is not 0-1 */
  val allowedRangePattern = """^0-(?!1$)""".r
  val onlyDownloads: AcastLog => Boolean = log =>
    log.status != "206" || allowedRangePattern.findFirstIn(log.range).nonEmpty

  /* filter out error reponses */
  val onlySuccessfulReponses: AcastLog => Boolean = { log => log.cloudfrontResponseResultType == "Hit" || log.cloudfrontResponseResultType == "RefreshHit" || log.cloudfrontResponseResultType == "Miss" }

  /* filter out HEAD and any non GET requests */
  val onlyGet: AcastLog => Boolean = { log => log.request == "GET" }

}

