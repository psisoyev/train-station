import sbt._
import Settings._

lazy val domain = project
  .settings(commonSettings)
  .settings(libraryDependencies ++= domainDependencies)

lazy val service = project
  .settings(commonSettings)
  .settings(libraryDependencies ++= serviceDependencies)
  .dependsOn(domain)

lazy val route = project
  .settings(commonSettings)
  .settings(libraryDependencies ++= routeDependencies)
  .dependsOn(service)

lazy val server = project
  .settings(commonSettings)
  .settings(libraryDependencies ++= serverDependencies)
  .dependsOn(route)

lazy val `train-station` = Project("train-station", file("."))
  .enablePlugins(JavaAppPackaging)
  .enablePlugins(AshScriptPlugin)
  .enablePlugins(DockerPlugin)
  .settings(commonSettings)
  .settings(organization := "psisoyev.io")
  .settings(moduleName := "train-station")
  .settings(name := "train-station")
  .aggregate(
    domain,
    service,
    route,
    server
  )
  .dependsOn(
    domain,
    service,
    route,
    server
  )
