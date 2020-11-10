package com.psisoyev.train.station

import com.psisoyev.train.station.Event.Departed
import zio.random.Random
import zio.test.Gen._
import zio.test.{ Gen, Sized }

object Generators {
  val trainId: Gen[Random with Sized, TrainId]             = alphaNumericString.map(TrainId(_))
  val city: Gen[Random with Sized, City]                   = alphaNumericString.map(City(_))
  val from: Gen[Random with Sized, From]                   = city.map(From(_))
  val to: Gen[Random with Sized, To]                       = city.map(To(_))
  val expected: Gen[Random, Expected]                      = anyInstant.map(Expected(_))
  val actual: Gen[Random, Actual]                          = anyInstant.map(Actual(_))
  val timestamp: Gen[Random, Timestamp]                    = anyInstant.map(Timestamp(_))
  val eventId: Gen[Random, EventId]                        = anyUUID.map(EventId(_))
  val departed: Gen[Random with Sized, Departed]           = for {
    id  <- eventId
    tId <- trainId
    f   <- from
    t   <- to
    e   <- expected
    ts  <- timestamp
  } yield Departed(id, tId, f, t, e, ts)
  val departedList: Gen[Random with Sized, List[Departed]] = listOf(departed)
}
