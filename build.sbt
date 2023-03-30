organization  := "com.gu"
description   := "AWS Lambda providing monitoring for podcasts consumption"
scalacOptions += "-deprecation"
scalaVersion  := "2.12.17"
scalacOptions ++= Seq("-deprecation", "-feature", "-unchecked", "-target:jvm-1.8", "-Xfatal-warnings")
name := "podcasts-analytics-lambda"

lazy val root = (project in file(".")).enablePlugins(JavaAppPackaging, RiffRaffArtifact)

libraryDependencies ++= Seq(
  "org.joda" % "joda-convert" % "2.2.2",
  "org.apache.logging.log4j" %% "log4j-api-scala" % "12.0",
  "com.amazonaws" % "aws-lambda-java-log4j2" % "1.5.1",
  "com.amazonaws" % "aws-lambda-java-core" % "1.2.2",
  "com.amazonaws" % "aws-java-sdk-s3" % "1.12.429",
  "com.amazonaws" % "aws-lambda-java-events" % "3.11.1",
  "com.github.melrief" %% "purecsv" % "0.1.1",
  "com.gu" %% "content-api-client-default" % "19.2.1",
  "com.squareup.okhttp3" % "okhttp" % "4.10.0",
  "net.openhft" % "zero-allocation-hashing" % "0.16",
  "org.scalatest" %% "scalatest" % "3.0.5" % "test"
)

Universal / topLevelDirectory := None
Universal / packageName := normalizedName.value

def env(key: String): Option[String] = Option(System.getenv(key))

riffRaffPackageType := (Universal / packageBin).value
riffRaffUploadArtifactBucket := Option("riffraff-artifact")
riffRaffUploadManifestBucket := Option("riffraff-builds")
riffRaffManifestProjectName := s"Off-platform::${name.value}"
