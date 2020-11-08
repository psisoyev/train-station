package com.psisoyev.train.station.arrival

import cats.Monad
import cats.implicits._
import com.psisoyev.train.station.Event.Arrived
import com.psisoyev.train.station.arrival.Arrivals.{ Arrival, ArrivalError }
import com.psisoyev.train.station.arrival.ExpectedTrains.ExpectedTrain
import com.psisoyev.train.station.{ Actual, City, Event, EventId, Logger, To, TrainId, UUIDGen }
import cr.pulsar.Producer
import io.circe.Decoder
import io.circe.generic.semiauto._

trait Arrivals[F[_]] {
  def register(arrival: Arrival): F[Either[ArrivalError, Arrived]]
}

object Arrivals {
  sealed trait ArrivalError
  object ArrivalError {
    case class UnexpectedTrain(id: TrainId) extends ArrivalError
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
    def register(arrival: Arrival): F[Either[ArrivalError, Arrived]] = {
      def arrived(train: ExpectedTrain): F[Arrived] =
        F.newEventId.map {
          Arrived(
            _,
            arrival.trainId,
            train.from,
            To(city),
            train.time,
            arrival.time.toTimestamp
          )
        }

      expectedTrains
        .get(arrival.trainId)
        .flatMap {
          case None =>
            val e: ArrivalError = ArrivalError.UnexpectedTrain(arrival.trainId)
            F.error(s"Tried to create arrival of an unexpected train: $arrival")
              .as(e.asLeft)
          case Some(train) =>
            arrived(train)
              .flatTap(a => expectedTrains.remove(a.trainId))
              .flatTap(producer.send_)
              .map(_.asRight[ArrivalError])
        }
    }
  }
}
