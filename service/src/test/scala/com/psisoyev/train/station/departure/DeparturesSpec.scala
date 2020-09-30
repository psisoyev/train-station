package com.psisoyev.train.station.departure

import com.psisoyev.train.station.Event.Departed
import com.psisoyev.train.station.{ BaseSpec, EventId, From }
import com.psisoyev.train.station.Generators.{ actual, city, expected, to, trainId }
import com.psisoyev.train.station.departure.Departures.Departure
import zio.interop.catz._
import zio.interop.catz.implicits._
import zio.test.Assertion.{ equalTo, isRight, isTrue }
import zio.test.environment.TestEnvironment
import zio.test.{ assert, checkM, suite, testM, ZSpec }

object DeparturesSpec extends BaseSpec {
  override def spec: ZSpec[TestEnvironment, Failure] =
    suite("DeparturesSpec")(
      testM("Register departing train") {
        checkM(trainId, to, city, expected, actual) { (trainId, to, city, expected, actual) =>
          for {
            (events, producer) <- fakeProducer[F]
            departures         = Departures.make[F](city, producer)
            result             <- departures.register(Departure(trainId, to, expected, actual))
            newEvents          <- events.get
          } yield {
            val departed = Departed(eventId, trainId, From(city), to, expected, actual.toTimestamp)
            assert(result)(isRight(equalTo(departed))) &&
            assert(result.toSeq == newEvents)(isTrue)
          }
        }
      }
    )
}
