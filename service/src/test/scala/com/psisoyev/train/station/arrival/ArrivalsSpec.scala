package com.psisoyev.train.station.arrival

import cats.effect.Ref
import com.psisoyev.train.station.Event.Arrived
import com.psisoyev.train.station.Generators._
import com.psisoyev.train.station.Logger._
import com.psisoyev.train.station.arrival.Arrivals.Arrival
import com.psisoyev.train.station.arrival.Arrivals.ArrivalError.UnexpectedTrain
import com.psisoyev.train.station.arrival.ExpectedTrains.ExpectedTrain
import com.psisoyev.train.station.{ BaseSpec, To }
import zio.interop.catz._
import zio.interop.catz.implicits._
import zio.test.Assertion._
import zio.test._
import zio.test.environment.TestEnvironment

object ArrivalsSpec extends BaseSpec {
  override def spec: ZSpec[TestEnvironment, Failure] =
    suite("ArrivalsSpec")(
      testM("Register expected train") {
        checkM(trainId, from, city, expected, actual) { (trainId, from, city, expected, actual) =>
          val expectedTrains = Map(trainId -> ExpectedTrain(from, expected))

          for {
            ref                <- Ref.of[F, ExpectedTrains](expectedTrains)
            expectedTrains      = ExpectedTrains.make[F](ref)
            (events, producer) <- fakeProducer[F]
            arrivals            = Arrivals.make[F](city, producer, expectedTrains)
            result             <- arrivals.register(Arrival(trainId, actual))
            newEvents          <- events.get
          } yield {
            val arrived = Arrived(eventId, trainId, from, To(city), expected, actual.toTimestamp)
            assert(result)(isRight(equalTo(arrived))) &&
            assert(result.toSeq == newEvents)(isTrue)
          }
        }
      },
      testM("Reject unexpected train") {
        checkM(trainId, city, actual) { (trainId, city, actual) =>
          for {
            ref                <- Ref.of[F, ExpectedTrains](Map.empty)
            expectedTrains      = ExpectedTrains.make[F](ref)
            (events, producer) <- fakeProducer[F]
            arrivals            = Arrivals.make[F](city, producer, expectedTrains)
            result             <- arrivals.register(Arrival(trainId, actual))
            newEvents          <- events.get
          } yield assert(result)(isLeft(equalTo(UnexpectedTrain(trainId)))) &&
            assert(newEvents)(isEmpty)
        }
      }
    )
}
