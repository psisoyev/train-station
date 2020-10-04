package com.psisoyev.train.station.departure

import cats.Monad
import cats.implicits._
import com.psisoyev.train.station.Event.Departed
import com.psisoyev.train.station.Logger._
import com.psisoyev.train.station._
import com.psisoyev.train.station.departure.Departures.{ Departure, ValidationError }
import cr.pulsar.Producer
import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder

trait Departures[F[_]] {
  def register(departure: Departure): F[Either[ValidationError, Departed]]
}

object Departures {
  sealed trait ValidationError

  case class Departure(id: TrainId, to: To, time: Expected, actual: Actual)
  object Departure {
    implicit val departureDecoder: Decoder[Departure] = deriveDecoder
  }

  def make[F[_]: Monad: UUIDGen: Logger](
    city: City,
    producer: Producer[F, Event]
  ): Departures[F] = new Departures[F] {
    override def register(departure: Departure): F[Either[ValidationError, Departed]] = {
      def departed: EventId => Departed =
        Departed(
          _,
          departure.id,
          From(city),
          departure.to,
          departure.time,
          departure.actual.toTimestamp
        )

      F.newEventId
        .map(departed)
        .flatTap(producer.send_)
        .map(_.asRight)
    }
  }
}
