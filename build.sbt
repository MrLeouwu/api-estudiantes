ThisBuild / version := "0.1.0"
ThisBuild / scalaVersion := "2.13.12"

lazy val root = (project in file("."))
  .enablePlugins(sbtassembly.AssemblyPlugin)
  .settings(
    name := "api-estudiantes",

    libraryDependencies ++= Seq(
      "org.http4s" %% "http4s-ember-server" % "0.23.26",
      "org.http4s" %% "http4s-dsl" % "0.23.26",
      "org.http4s" %% "http4s-circe" % "0.23.26",
      "io.circe" %% "circe-generic" % "0.14.6",
      "org.tpolecat" %% "doobie-core" % "1.0.0-RC5",
      "org.tpolecat" %% "doobie-hikari" % "1.0.0-RC5",
      "com.mysql" % "mysql-connector-j" % "8.3.0",
      "org.slf4j" % "slf4j-simple" % "2.0.12",

      // TAPIR + Swagger
      "com.softwaremill.sttp.tapir" %% "tapir-core" % "1.9.10",
      "com.softwaremill.sttp.tapir" %% "tapir-http4s-server" % "1.9.10",
      "com.softwaremill.sttp.tapir" %% "tapir-openapi-docs" % "1.9.10",
      "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui-bundle" % "1.9.10",
      "com.softwaremill.sttp.tapir" %% "tapir-json-circe" % "1.9.10"
    ),

    Compile / mainClass := Some("main.Main"),
    assembly / mainClass := Some("main.Main"),

    assembly / assemblyMergeStrategy := {
      case PathList("META-INF", "services", _ @ _*) =>
        MergeStrategy.concat
      case PathList("META-INF", "resources", "webjars", _ @ _*) =>
        MergeStrategy.first
      case PathList("META-INF", _ @ _*) =>
        MergeStrategy.first
      case _ =>
        MergeStrategy.first
    }
  )