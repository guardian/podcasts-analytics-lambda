organization  := "com.gu"
description   := "AWS Lambda providing monitoring for podcasts consumption"
scalacOptions += "-deprecation"
scalaVersion  := "2.13.14"
scalacOptions ++= Seq("-deprecation", "-feature", "-unchecked", "-release:11", "-Xfatal-warnings", "-Xlint:unused")
name := "podcasts-analytics-lambda"

enablePlugins(JavaAppPackaging)
Universal / topLevelDirectory := None
Universal / packageName := normalizedName.value

val awsVersion = "2.27.8"
val log4jVersion = "2.25.5"

dependencyOverrides ++= Seq(
  "org.apache.logging.log4j" % "log4j-api" % log4jVersion,
  "org.apache.logging.log4j" % "log4j-core" % log4jVersion,
  "org.apache.logging.log4j" % "log4j-layout-template-json" % log4jVersion
)

dependencyOverrides += "org.apache.thrift" % "libthrift" % "0.24.0"

// Stay on the AWS SDK's Netty 4.1 line, keeping native and Java modules aligned.
dependencyOverrides ++= Seq(
  "netty-buffer",
  "netty-codec",
  "netty-codec-http",
  "netty-codec-http2",
  "netty-common",
  "netty-handler",
  "netty-resolver",
  "netty-transport",
  "netty-transport-classes-epoll",
  "netty-transport-native-unix-common"
).map("io.netty" % _ % "4.1.137.Final")

dependencyOverrides ++= Seq(
  "org.apache.httpcomponents.client5" % "httpclient5" % "5.6.3",
  "org.apache.httpcomponents.core5" % "httpcore5" % "5.4.3",
  "org.apache.httpcomponents.core5" % "httpcore5-h2" % "5.4.3"
)

libraryDependencies ++= Seq(
  "org.joda" % "joda-convert" % "2.2.2",
  "org.apache.logging.log4j" %% "log4j-api-scala" % "13.1.0",
  "com.amazonaws" % "aws-lambda-java-log4j2" % "1.6.0",
  "com.amazonaws" % "aws-lambda-java-core" % "1.2.3",
  "org.apache.logging.log4j" % "log4j-layout-template-json" % log4jVersion,
  "software.amazon.awssdk" % "s3" % awsVersion,
  "software.amazon.awssdk" % "s3-transfer-manager" % awsVersion,
  "com.amazonaws" % "aws-lambda-java-events" % "3.13.0",
  "io.kontainers" %% "purecsv" % "1.3.10",
  "com.gu" %% "content-api-client-default" % "30.0.0",
  "com.squareup.okhttp3" % "okhttp" % "4.10.0",
  "net.openhft" % "zero-allocation-hashing" % "0.6",
  "org.scalatest" %% "scalatest" % "3.2.19" % "test"
)

def env(key: String): Option[String] = Option(System.getenv(key))


Test / testOptions += Tests.Argument(TestFrameworks.ScalaTest, "-u", sys.env.getOrElse("SBT_JUNIT_OUTPUT", "junit"))
