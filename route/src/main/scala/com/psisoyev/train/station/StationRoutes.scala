package com.psisoyev.train.station

import cats.{ Defer, MonadError }
import cats.implicits._
import com.psisoyev.train.station.arrival.Arrivals
import com.psisoyev.train.station.arrival.Arrivals.{ Arrival, ArrivalError }
import com.psisoyev.train.station.departure.Departures
import com.psisoyev.train.station.departure.Departures.{ Departure, DepartureError }
import org.http4s.dsl.Http4sDsl
import org.http4s.{ HttpRoutes, _ }
import org.http4s.circe._
import org.http4s.circe.CirceEntityEncoder._

class StationRoutes[F[_]: MonadError[*[_], Throwable]: Defer: JsonDecoder](
  arrivals: Arrivals[F],
  departures: Departures[F]
) extends Http4sDsl[F] {
  val routes: HttpRoutes[F] = HttpRoutes.of[F] {
    case req @ POST -> Root / "arrival"   =>
      req
        .asJsonDecode[Arrival]
        .flatMap(arrivals.register)
        .flatMap(_.fold(handleArrivalErrors, _ => Ok()))
    case req @ POST -> Root / "departure" =>
      req
        .asJsonDecode[Departure]
        .flatMap(departures.register)
        .flatMap(_.fold(handleDepartureErrors, _ => Ok()))

  }

  def handleArrivalErrors: ArrivalError => F[Response[F]] = { case ArrivalError.UnexpectedTrain(id) =>
    BadRequest(s"Unexpected train $id")
  }

  def handleDepartureErrors: DepartureError => F[Response[F]] = { case DepartureError.UnexpectedDestination(city) =>
    BadRequest(s"Unexpected city $city")
  }
}
