import sbt._

object Dependencies {
  val zioCore = "dev.zio" %% "zio" % Version.zio
  val zioCats = ("dev.zio" %% "zio-interop-cats" % Version.zioCats).excludeAll(ExclusionRule("dev.zio"))
  val zio = List(zioCore, zioCats)

  val cats = "org.typelevel" %% "cats-core" % Version.cats
  val catsEffect = "org.typelevel" %% "cats-effect" % Version.cats

  val zioTestCore = "dev.zio" %% "zio-test"    % Version.zio % Test
  val zioTestSbt = "dev.zio" %% "zio-test-sbt" % Version.zio % Test
  val zioTest = List(zioTestCore, zioTestSbt)

  val http4s = Seq(
    "org.http4s" %% "http4s-dsl" % Version.http4s,
    "org.http4s" %% "http4s-circe" % Version.http4s,
    "org.http4s" %% "http4s-blaze-server" % Version.http4s
  )

  val fs2Core = "co.fs2" %% "fs2-core" % Version.fs2Core

  val newtype = "io.estatico" %% "newtype" % Version.newtype

  val doobieCore = "org.tpolecat" %% "doobie-core" % Version.doobie
  val doobieH2 = "org.tpolecat" %% "doobie-h2" % Version.doobie
  val doobieHikari = "org.tpolecat" %% "doobie-hikari" % Version.doobie
  val doobie = List(doobieCore, doobieH2, doobieHikari)

  val circeGeneric = "io.circe" %% "circe-generic" % Version.circe
  val circeCore = "io.circe" %% "circe-core" % Version.circe
  val circeParser = "io.circe" %% "circe-parser" % Version.circe
  val circe = List(circeGeneric, circeCore, circeParser)

  val slf4j = "org.slf4j" % "slf4j-simple" % Version.slf4j

  val neutronCore = "com.chatroulette" %% "neutron-core" % Version.neutron
  val neutronCirce = "com.chatroulette" %% "neutron-circe" % Version.neutron
  val neutronFunction = "com.chatroulette" %% "neutron-function" % Version.neutron

  val ciris = "is.cir" %% "ciris" % Version.ciris

  val contextApplied = "org.augustjune" %% "context-applied" % Version.contextApplied
}

object Version {
  val cats = "2.2.0"
  val zio = "1.0.1"
  val zioCats = "2.1.4.0"
  val slf4j = "1.7.28"
  val fs2Core = "2.4.2"
  val kindProjector = "0.10.3"
  val http4s = "1.0.0-M4"
  val ciris = "1.2.0"
  val circe = "0.13.0"
  val doobie = "0.9.0"
  val newtype = "0.4.4"
  val neutron = "0.0.2+37-d92ef362-SNAPSHOT"
  val contextApplied = "0.1.3"
}
