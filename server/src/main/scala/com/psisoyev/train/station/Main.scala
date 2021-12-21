package com.psisoyev.train.station

import com.psisoyev.train.station.Event.Departed
import com.psisoyev.train.station.UUIDGen._
import com.psisoyev.train.station.arrival.{ Arrivals, ExpectedTrains }
import com.psisoyev.train.station.departure.{ DepartureTracker, Departures }
import fs2.Stream
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.implicits._
import zio._
import zio.interop.catz._
import zio.interop.catz.implicits._

object Main extends zio.App {
  type F[A] = Task[A]

  override def run(args: List[String]): ZIO[ZEnv, Nothing, ExitCode] =
    Resources
      .make[F, Event]
      .use { case Resources(config, producer, consumers, trainRef) =>
        val expectedTrains   = ExpectedTrains.make[F](trainRef)
        val arrivals         = Arrivals.make[F](config.city, producer, expectedTrains)
        val departures       = Departures.make[F](config.city, config.connectedTo, producer)
        val departureTracker = DepartureTracker.make[F](config.city, expectedTrains)

        val routes = new StationRoutes[F](arrivals, departures).routes.orNotFound

        val httpServer =
          BlazeServerBuilder[F]
            .bindHttp(config.httpPort.value, "0.0.0.0")
            .withHttpApp(routes)
            .serve
            .compile
            .drain

        val departureListener =
          Stream
            .emits(consumers)
            .map(_.autoSubscribe)
            .parJoinUnbounded
            .collect { case e: Departed => e }
            .evalMap(departureTracker.save)
            .compile
            .drain

        Logger[F].info(s"Started train station ${config.city}") *>
          departureListener
            .zipPar(httpServer)
            .unit
      }
      .exitCode
}
