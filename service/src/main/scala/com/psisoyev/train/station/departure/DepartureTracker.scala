package com.psisoyev.train.station.departure

import cats.Functor
import cats.implicits._
import com.psisoyev.train.station.Event.Departed
import com.psisoyev.train.station.arrival.ExpectedTrains
import com.psisoyev.train.station.arrival.ExpectedTrains.ExpectedTrain
import com.psisoyev.train.station.{ City, Event, Logger }
import fs2.Pipe

trait DepartureTracker[F[_]] {
  def run: Pipe[F, Event, Departed]
}

object DepartureTracker {
  def make[F[_]: Functor: Logger](
    city: City,
    expectedTrains: ExpectedTrains[F]
  ): DepartureTracker[F] = new DepartureTracker[F] {
    def log(e: Event.Departed): F[Unit] =
      F.info(s"$city is expecting ${e.trainId} from ${e.from} at ${e.expected}")
    def add(e: Event.Departed): F[Unit] =
      expectedTrains.update(e.trainId, ExpectedTrain(e.from, e.expected))

    def run: Pipe[F, Event, Departed] =
      _.collect { case e: Event.Departed if e.to.city === city => e }
        .evalTap(log)
        .evalTap(add)
  }
}
