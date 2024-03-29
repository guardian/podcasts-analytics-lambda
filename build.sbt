organization  := "com.gu"
description   := "AWS Lambda providing monitoring for podcasts consumption"
scalacOptions += "-deprecation"
scalaVersion  := "2.13.10"
scalacOptions ++= Seq("-deprecation", "-feature", "-unchecked", "-release:11", "-Xfatal-warnings")
name := "podcasts-analytics-lambda"

enablePlugins(JavaAppPackaging)
Universal / topLevelDirectory := None
Universal / packageName := normalizedName.value

libraryDependencies ++= Seq(
  "org.joda" % "joda-convert" % "2.2.2",
  "org.apache.logging.log4j" %% "log4j-api-scala" % "12.0",
  "com.amazonaws" % "aws-lambda-java-log4j2" % "1.5.1",
  "com.amazonaws" % "aws-lambda-java-core" % "1.2.2",
  "com.amazonaws" % "aws-java-sdk-s3" % "1.12.641",
  "com.amazonaws" % "aws-lambda-java-events" % "3.11.1",
  "io.kontainers" %% "purecsv" % "1.3.10",
  "com.gu" %% "content-api-client-default" % "19.2.1",
  "com.squareup.okhttp3" % "okhttp" % "4.10.0",
  "net.openhft" % "zero-allocation-hashing" % "0.6",
  "org.scalatest" %% "scalatest" % "3.2.15" % "test"
)

def env(key: String): Option[String] = Option(System.getenv(key))


Test / testOptions += Tests.Argument(TestFrameworks.ScalaTest, "-u", sys.env.getOrElse("SBT_JUNIT_OUTPUT", "junit"))
