name := "fs2exercise"

version := "0.1"

scalaVersion := "2.12.8"

lazy val calendar_api_sample = project.in(file("calendar_api_sample"))
  .settings(name := "calendar_api_sample")
  .settings(
    libraryDependencies ++= Seq(
      "co.fs2" %% "fs2-core" % "1.0.4",
      "co.fs2" %% "fs2-io" % "1.0.4",
      "com.google.api-client"   % "google-api-client" % "1.28.0",
      "com.google.oauth-client" % "google-oauth-client-jetty" % "1.29.0",
      "com.google.apis"         % "google-api-services-calendar" % "v3-rev378-1.25.0"
    ),
    scalacOptions ++= Seq(
      "-encoding", "utf8", // Option and arguments on same line
      "-Xfatal-warnings",  // New lines for each options
      "-deprecation",
      "-unchecked",
      "-language:implicitConversions",
      "-language:higherKinds",
      "-language:existentials",
      "-language:postfixOps"
    )
  )