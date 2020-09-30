package com.psisoyev.train.station

import io.circe._
import io.circe.generic.semiauto._

sealed trait Event {
  def id: EventId
  def trainId: TrainId
  def created: Timestamp
}

object Event {
  final case class Departed(id: EventId, trainId: TrainId, from: From, to: To, expected: Expected, created: Timestamp) extends Event
  final case class Arrived(id: EventId, trainId: TrainId, from: From, to: To, expected: Expected, created: Timestamp)  extends Event

  implicit val eventEncoder: Encoder[Event] = deriveEncoder
  implicit val eventDecoder: Decoder[Event] = deriveDecoder
}
