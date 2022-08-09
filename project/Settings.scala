import Dependencies._
import sbt.Keys.{ scalacOptions, _ }
import sbt._

object Settings {

  val commonSettings =
    Seq(
      scalaVersion := "2.13.8",
      scalacOptions := Seq(
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
      testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework"),
      cancelable in Global := true,
      fork in Global := true, // https://github.com/sbt/sbt/issues/2274
      Compile / mainClass := Some("com.psisoyev.train.station.Main"),
      addCompilerPlugin(contextApplied),
      addCompilerPlugin(kindProjector)
    )

  val serviceDependencies = List(cats, catsEffect, neutronCore, slf4j, zioCats) ++ zioTest
  val routeDependencies   = http4s
  val serverDependencies  = List(neutronCirce, ciris) ++ zio
  val domainDependencies  = List(newtype) ++ circe
}
