lazy val akkaHttpVersion = "10.2.2"
lazy val akkaVersion = "2.6.10"

lazy val root = (project in file("."))
  .aggregate(wsClient)

lazy val wsClient = project
  .settings(
    inThisBuild(
      List(
        organization := "com.verneek",
        scalaVersion := "2.13.4"
      )
    ),
    name := "wsClient",
    resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots",
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-http-xml" % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-stream" % akkaVersion
    )
  )
