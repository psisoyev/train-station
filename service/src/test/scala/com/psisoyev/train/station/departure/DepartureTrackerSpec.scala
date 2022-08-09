package com.psisoyev.train.station.departure

import cats.effect.kernel.Ref
import cats.syntax.all._
import com.psisoyev.train.station.BaseSpec
import com.psisoyev.train.station.Generators._
import com.psisoyev.train.station.Logger._
import com.psisoyev.train.station.arrival.ExpectedTrains
import zio.interop.catz._
import zio.interop.catz.implicits._
import zio.test.Assertion.equalTo
import zio.test.{assert, check}

object DepartureTrackerSpec extends BaseSpec {
  override def spec =
    suite("DepartureTrackerSpec")(
      test("Expect trains departing to $city") {
        check(departedList, city) { (departed, city) =>
          for {
            ref           <- Ref.of[F, ExpectedTrains](Map.empty)
            expectedTrains = ExpectedTrains.make[F](ref)
            tracker        = DepartureTracker.make[F](city, expectedTrains)
            _             <- departed.traverse(tracker.save)
            result        <- ref.get
          } yield assert(result.size)(equalTo(departed.count(_.to.city === city)))
        }
      }
    )
}
