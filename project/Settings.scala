import Dependencies._
import com.typesafe.sbt.packager.Keys._
import sbt.Keys.{ scalacOptions, _ }
import sbt._

object Settings {

  val commonSettings =
    Seq(
      scalaVersion         := "2.13.14",
      scalacOptions        := Seq(
        "-Ymacro-annotations",
        "-deprecation",
        "-encoding",
        "utf-8",
        "-explaintypes",
        "-feature",
        "-unchecked",
        "-language:postfixOps",
        "-language:higherKinds",
        "-language:implicitConversions",
        "-Xcheckinit",
        "-Xfatal-warnings"
      ),
      version              := (version in ThisBuild).value,
      testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework"),
      cancelable in Global := true,
      fork in Global       := true, // https://github.com/sbt/sbt/issues/2274
      mainClass in Compile := Some("com.psisoyev.train.station.Main"),
      addCompilerPlugin(contextApplied),
      addCompilerPlugin(kindProjector),
      addCompilerPlugin(bm4),
      dockerBaseImage      := "openjdk:11-jre",
      dockerUpdateLatest   := true
    )

  val serviceDependencies = List(cats, catsEffect, neutronCore, slf4j, zioCats) ++ zioTest
  val routeDependencies   = http4s
  val serverDependencies  = List(neutronCirce, ciris) ++ zio
  val domainDependencies  = List(newtype) ++ circe
}
