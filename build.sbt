organization  := "com.gu"
description   := "AWS Lambda providing monitoring for podcasts consumption"
scalacOptions += "-deprecation"
scalaVersion  := "2.11.8"
scalacOptions ++= Seq("-deprecation", "-feature", "-unchecked", "-target:jvm-1.8", "-Xfatal-warnings")
name := "podcasts-analytics-lambda"

lazy val root = (project in file(".")).enablePlugins(JavaAppPackaging, RiffRaffArtifact)

libraryDependencies ++= Seq(
  "org.joda" % "joda-convert" % "1.8.1",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.4.0",
  "ch.qos.logback" %  "logback-classic" % "1.1.7",
  "com.amazonaws" % "aws-lambda-java-core" % "1.1.0",
  "com.amazonaws" % "aws-java-sdk-s3" % "1.11.13",
  "com.amazonaws" % "aws-lambda-java-events" % "1.0.0",
  "com.github.melrief" %% "purecsv" % "0.0.6",
  "org.scalatest" %% "scalatest" % "2.2.5" % "test"
)

topLevelDirectory in Universal := None
packageName in Universal := normalizedName.value

def env(key: String): Option[String] = Option(System.getenv(key))

riffRaffPackageName := "podcasts-analytics-lambda"
riffRaffPackageType := (packageBin in Universal).value
riffRaffUploadArtifactBucket := Option("riffraff-artifact")
riffRaffUploadManifestBucket := Option("riffraff-builds")
riffRaffManifestProjectName := s"Off-platform::${name.value}"
riffRaffManifestVcsUrl := "git@github.com:guardian/podcasts-analytics-lambda.git"
riffRaffManifestBranch := env("BRANCH_NAME").getOrElse("unknown_branch")
riffRaffBuildIdentifier := env("BUILD_NUMBER").getOrElse("DEV")

assemblySettings
