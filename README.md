# podcasts-analytics-lambda

This lambda is responsible for sending podcasts metrics to Ophan, our real time analytics system.

It does so by mining our CDN logs (stored in an S3 bucket, `fastly-logs-audio`), collecting information about every single file hit and constructing a model out of every log line. If a log line is parsed correctly into the model, an event is sent to Ophan mimicking a pageview. Ophan has some ad-hoc logic and knows how to handle these special requests.

Recently we added support for processing log files coming from Acast as well.

### Architecture overview

```
+----------+      +-----------+                 +--------+                       +-------+
|  Fastly  | ---> | S3 bucket | -- triggers --> | Lambda | -- send events to --> | Ophan |
+----------+      +-----------+                 +--------+                       +-------+
```


### Run & Tests

To run locally you need to export a valid CAPI key as a local variable:

```
export CAPI_KEY='your-key'
sbt test
```

### Logs format

```
"" "%t" "req.request" "req.url" "req.http.host" "resp.status" "client.ip" "req.http.user-agent" "req.http.referer" "req.http.Range" "req.header_bytes_read" "req.body_bytes_read" "resp.http.Content-Type" "resp.http.Age" "geoip.country_code" "geoip.region" "geoip.city" "resp.http.Location"
```
