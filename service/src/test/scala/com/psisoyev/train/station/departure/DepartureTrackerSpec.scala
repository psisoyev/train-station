package com.psisoyev.train.station.departure

import cats.implicits._
import cats.effect.concurrent.Ref
import com.psisoyev.train.station.BaseSpec
import com.psisoyev.train.station.Generators._
import com.psisoyev.train.station.Logger._
import com.psisoyev.train.station.arrival.ExpectedTrains
import zio.interop.catz._
import zio.interop.catz.implicits._
import zio.test.environment.TestEnvironment
import zio.test.{ assert, checkM, suite, testM, ZSpec }
import fs2.Stream
import zio.test.Assertion.equalTo

object DepartureTrackerSpec extends BaseSpec {
  override def spec: ZSpec[TestEnvironment, Failure] =
    suite("DepartureTrackerSpec")(
      testM("Expect trains departing to $city") {
        checkM(departedList, city) { (departed, city) =>
          for {
            ref            <- Ref.of[F, ExpectedTrains](Map.empty)
            expectedTrains = ExpectedTrains.make[F](ref)
            tracker        = DepartureTracker.make[F](city, expectedTrains)
            _ <- Stream
                  .emits(departed)
                  .through(tracker.run)
                  .compile
                  .drain
            result <- ref.get
          } yield {
            assert(result.size)(equalTo(departed.count(_.to.city === city)))
          }
        }
      }
    )
}
