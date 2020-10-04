package com.psisoyev.train.station.departure

import cats.Applicative
import cats.implicits._
import com.psisoyev.train.station.Event.Departed
import com.psisoyev.train.station.arrival.ExpectedTrains
import com.psisoyev.train.station.arrival.ExpectedTrains.ExpectedTrain
import com.psisoyev.train.station.{ City, Logger }

trait DepartureTracker[F[_]] {
  def save(e: Departed): F[Unit]
}

object DepartureTracker {
  def make[F[_]: Applicative: Logger](
    city: City,
    expectedTrains: ExpectedTrains[F]
  ): DepartureTracker[F] = new DepartureTracker[F] {
    def save(e: Departed): F[Unit] =
      if (e.to.city =!= city) F.unit
      else
        expectedTrains.update(e.trainId, ExpectedTrain(e.from, e.expected)) *>
          F.info(s"$city is expecting ${e.trainId} from ${e.from} at ${e.expected}")
  }
}
