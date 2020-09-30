package com.psisoyev.train.station

import java.util.UUID
import cats.implicits._
import cats.effect.Sync

trait UUIDGen[F[_]] {
  def random: F[UUID]
  def newEventId: F[EventId]
}

/**
 * Naive implementation of UUID generator, not for production use
 */
object UUIDGen {
  def apply[F[_]](implicit uuidGen: UUIDGen[F]): UUIDGen[F] = uuidGen

  implicit def instance[F[_]: Sync]: UUIDGen[F] = new UUIDGen[F] {
    def random: F[UUID]        = F.delay(UUID.randomUUID())
    def newEventId: F[EventId] = random.map(EventId(_))
  }
}
