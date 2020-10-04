package com.psisoyev.train.station.arrival

import cats.Monad
import cats.implicits._
import com.psisoyev.train.station.Event.Arrived
import com.psisoyev.train.station.arrival.Arrivals.{ Arrival, ValidationError }
import com.psisoyev.train.station.{ Actual, City, Event, EventId, Logger, To, TrainId, UUIDGen }
import cr.pulsar.Producer
import io.circe.{ Decoder, Encoder }
import io.circe.generic.semiauto._

trait Arrivals[F[_]] {
  def register(arrival: Arrival): F[Either[ValidationError, Arrived]]
}

object Arrivals {
  sealed trait ValidationError
  object ValidationError {
    case class UnexpectedTrain(id: TrainId) extends ValidationError
  }

  case class Arrival(trainId: TrainId, time: Actual)
  object Arrival {
    implicit val arrivalDecoder: Decoder[Arrival] = deriveDecoder
  }

  def make[F[_]: Monad: UUIDGen: Logger](
    city: City,
    producer: Producer[F, Event],
    expectedTrains: ExpectedTrains[F]
  ): Arrivals[F] = new Arrivals[F] {
    def register(arrival: Arrival): F[Either[ValidationError, Arrived]] =
      expectedTrains
        .get(arrival.trainId)
        .flatMap {
          case None =>
            val e: ValidationError = ValidationError.UnexpectedTrain(arrival.trainId)
            F.info(s"Unexpected error has occurred: $e")
              .as(e.asLeft)
          case Some(train) =>
            def arrived: EventId => Arrived =
              Arrived(
                _,
                arrival.trainId,
                train.from,
                To(city),
                train.time,
                arrival.time.toTimestamp
              )

            F.newEventId
              .map(arrived)
              .flatTap(a => expectedTrains.remove(a.trainId))
              .flatTap(producer.send_)
              .map(_.asRight)
        }
  }
}
