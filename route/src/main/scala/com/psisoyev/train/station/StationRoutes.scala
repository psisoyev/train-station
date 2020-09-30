package com.psisoyev.train.station

import cats.effect.Sync
import cats.implicits._
import com.psisoyev.train.station.arrival.Arrivals
import com.psisoyev.train.station.arrival.Arrivals.{ Arrival, ValidationError }
import com.psisoyev.train.station.departure.Departures
import com.psisoyev.train.station.departure.Departures.Departure
import io.circe._
import org.http4s.circe.CirceEntityCodec._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s.{ HttpRoutes, _ }

class StationRoutes[F[_]: Sync](
  arrivals: Arrivals[F],
  departures: Departures[F]
) {
  implicit def circeJsonDecoder[A](implicit decoder: Decoder[A]): EntityDecoder[F, A] = jsonOf[F, A]

  private val dsl = Http4sDsl[F]
  import dsl._

  val routes: HttpRoutes[F] = HttpRoutes.of[F] {
    case req @ POST -> Root / "arrival" =>
      for {
        arrival  <- req.as[Arrival]
        result   <- arrivals.register(arrival)
        response <- result.fold(handleErrors, _ => Ok())
      } yield response
    case req @ POST -> Root / "departure" =>
      for {
        departure <- req.as[Departure]
        _         <- departures.register(departure)
        response  <- Ok()
      } yield response
  }

  def handleErrors(error: ValidationError): F[Response[F]] = error match {
    case ValidationError.UnexpectedTrain(id) => BadRequest(s"Unexpected train $id")
  }
}
