# podcasts-analytics-lambda

This lambda is responsible for sending podcasts metrics to Ophan, our real time analytics system.

It does so by mining our CDN logs (stored in an S3 bucket), collecting information about every single file hit and constructing a model out of every log line. If a log line is parsed correctly into the model, an event is sent to Ophan mimicking a pageview. Ophan has some ad-hoc logic and knows how to handle these special requests.

This lambda currently pulls audio data from logs provided by Fastly and Acast.

### Architecture overview

```
+---------------+      +------------+                 +--------+                       +-------+
| Fastly/Acast  | ---> | S3 bucket* | -- triggers --> | Lambda | -- send events to --> | Ophan |
+---------------+      +------------+                 +--------+                       +-------+
```
* The fastly and acast logs arrive in separate buckets, and [another lambda](https://github.com/guardian/s3-chunking-lambda) copies them into a single bucket. Any log files above a certain size will be split into several files to ensure this lambda does not run out of time.

### Run & Tests

To run locally you need to export a valid PROD CAPI key as a local variable:

```
export CAPI_KEY='your-PROD-key'
sbt test
```

### Monitoring
The lambda logs warnings and errors to cloudwatch. E.g. a failed capi request is ERROR level, and failure to parse a single line is WARN level.

Cloudwatch alarms are triggered if:
- the lambda invocation rate drops below 200/hour
- the lambda fails with an error (e.g. out of memory, timeout)
- the lambda logs any errors or warnings.
