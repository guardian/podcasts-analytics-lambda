# podcasts-analytics-lambda

This lambda is responsible for sending podcasts metrics to Ophan, our real time analytics system.

It does so by mining our CDN logs (stored in an S3 bucket), collecting information about every single file hit and constructing a model out of every log line. If a log line is parsed correctly into the model, an event is sent to Ophan mimicking a pageview. Ophan has some ad-hoc logic and knows how to handle these special requests.
