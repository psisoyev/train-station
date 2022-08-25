package com.psisoyev.train.station.departure

import com.psisoyev.train.station.Event.Departed
import com.psisoyev.train.station.Generators._
import com.psisoyev.train.station.departure.Departures.{ Departure, DepartureError }
import com.psisoyev.train.station.{ BaseSpec, From }
import zio.interop.catz._
import zio.interop.catz.implicits._
import zio.test.Assertion._
import zio.test._

object DeparturesSpec extends BaseSpec {
  override def spec =
    suite("DeparturesSpec")(
      test("Fail to register departing train to an unexpected destination city") {
        check(trainId, to, city, expected, actual) { (trainId, to, city, expected, actual) =>
          for {
            (events, producer) <- fakeProducer[F]
            departures          = Departures.make[F](city, List(), producer)
            result             <- departures.register(Departure(trainId, to, expected, actual))
            newEvents          <- events.get
          } yield assert(result)(isLeft(equalTo(DepartureError.UnexpectedDestination(to.city)))) && assertTrue(newEvents.isEmpty)
        }
      },
      test("Register departing train") {
        check(trainId, to, city, expected, actual) { (trainId, to, city, expected, actual) =>
          for {
            (events, producer) <- fakeProducer[F]
            departures          = Departures.make[F](city, List(to.city), producer)
            result             <- departures.register(Departure(trainId, to, expected, actual))
            newEvents          <- events.get
          } yield {
            val departed = Departed(eventId, trainId, From(city), to, expected, actual.toTimestamp)
            assert(result)(isRight(equalTo(departed))) &&
            assertTrue(result.toSeq == newEvents)
          }
        }
      }
    )
}
