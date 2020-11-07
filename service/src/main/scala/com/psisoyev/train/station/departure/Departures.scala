package com.psisoyev.train.station.departure

import cats.Monad
import cats.implicits._
import com.psisoyev.train.station.Event.Departed
import com.psisoyev.train.station.Logger._
import com.psisoyev.train.station._
import com.psisoyev.train.station.departure.Departures.{ Departure, DepartureError }
import cr.pulsar.Producer
import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder

trait Departures[F[_]] {
  def register(departure: Departure): F[Either[DepartureError, Departed]]
}

object Departures {
  sealed trait DepartureError
  object DepartureError {
    case class UnexpectedDestination(city: City) extends DepartureError
  }

  case class Departure(id: TrainId, to: To, time: Expected, actual: Actual)
  object Departure {
    implicit val departureDecoder: Decoder[Departure] = deriveDecoder
  }

  def make[F[_]: Monad: UUIDGen: Logger](
    city: City,
    connectedTo: List[City],
    producer: Producer[F, Event]
  ): Departures[F] = new Departures[F] {
    override def register(departure: Departure): F[Either[DepartureError, Departed]] = {
      val destination = departure.to.city
      val departed: F[Either[DepartureError, Departed]] =
        F.newEventId
          .map {
            Departed(
              _,
              departure.id,
              From(city),
              departure.to,
              departure.time,
              departure.actual.toTimestamp
            )
          }
          .flatTap(producer.send_)
          .map(_.asRight)

      val error: F[Either[DepartureError, Departed]] = {
        val error: DepartureError = DepartureError.UnexpectedDestination(destination)
        F.error(s"Tried to departure to an unexpected destination: $departure")
          .as(error.asLeft)
      }

      connectedTo
        .find(_ === destination)
        .as(departed)
        .getOrElse(error)
    }
  }
}
