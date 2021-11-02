import Dependencies._
import com.typesafe.sbt.packager.Keys._
import org.scalafmt.sbt.ScalafmtPlugin.autoImport.scalafmtOnCompile
import sbt.Keys.{scalacOptions, _}
import sbt._

object Settings {

  val commonSettings = {
    Seq(
      scalaVersion := "2.13.7",
      scalacOptions := Seq(
        "-Ymacro-annotations",
        "-deprecation",
        "-encoding", "utf-8",
        "-explaintypes",
        "-feature",
        "-unchecked",
        "-language:postfixOps",
        "-language:higherKinds",
        "-language:implicitConversions",
        "-Xcheckinit",
        "-Xfatal-warnings"
      ),
      version := (version in ThisBuild).value,
      scalafmtOnCompile := true,
      testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework"),

      cancelable in Global := true,
      fork in Global := true, // https://github.com/sbt/sbt/issues/2274
      resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots",

      mainClass in Compile := Some("com.psisoyev.train.station.Main"),

      addCompilerPlugin(contextApplied),
      addCompilerPlugin(kindProjector),

      dockerBaseImage := "openjdk:jre-alpine",
      dockerUpdateLatest := true
    )
  }

  val serviceDependencies = List(cats, catsEffect, neutronCore, slf4j, zioCats) ++ zioTest
  val routeDependencies = http4s
  val serverDependencies = List(neutronCirce, ciris) ++ zio
  val domainDependencies = List(newtype) ++ circe
}
