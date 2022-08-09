package com.psisoyev.train.station

import com.psisoyev.train.station.Event.Departed
import zio.test.Gen._
import zio.test.{Gen, Sized}

object Generators {
  val trainId: Gen[Sized, TrainId]             = alphaNumericString.map(TrainId(_))
  val city: Gen[Sized, City]                   = alphaNumericString.map(City(_))
  val from: Gen[Sized, From]                   = city.map(From(_))
  val to: Gen[Sized, To]                       = city.map(To(_))
  val expected: Gen[Any, Expected]             = instant.map(Expected(_))
  val actual: Gen[Any, Actual]                 = instant.map(Actual(_))
  val timestamp: Gen[Any, Timestamp]           = instant.map(Timestamp(_))
  val eventId: Gen[Any, EventId]               = uuid.map(EventId(_))
  val departed: Gen[Sized, Departed]           = for {
    id  <- eventId
    tId <- trainId
    f   <- from
    t   <- to
    e   <- expected
    ts  <- timestamp
  } yield Departed(id, tId, f, t, e, ts)
  val departedList: Gen[Sized, List[Departed]] = listOf(departed)
}
