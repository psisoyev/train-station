import sbt._
import Settings._

lazy val domain = project
  .settings(commonSettings)
  .settings(libraryDependencies ++= domainDependencies)
  .settings(higherKinds)

lazy val function = project
  .settings(commonSettings)
  .settings(libraryDependencies ++= functionDependencies)
  .dependsOn(domain)

lazy val service = project
  .settings(commonSettings)
  .settings(libraryDependencies ++= serviceDependencies)
  .settings(higherKinds)
  .dependsOn(domain)

lazy val route = project
  .settings(commonSettings)
  .settings(libraryDependencies ++= routeDependencies)
  .settings(higherKinds)
  .dependsOn(service)

lazy val server = project
  .settings(commonSettings)
  .settings(libraryDependencies ++= serverDependencies)
  .settings(higherKinds)
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
    server,
    function
  )
  .dependsOn(
    domain,
    function,
    service,
    route,
    server
  )
