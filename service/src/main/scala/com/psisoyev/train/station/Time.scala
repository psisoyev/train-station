package com.psisoyev.train.station

import java.time.Instant
import java.util.concurrent.TimeUnit

import cats.Functor
import cats.effect.Clock
import cats.implicits._

trait Time[F[_]] {
  def timestamp: F[Timestamp]
}

object Time {

  def apply[F[_]](implicit time: Time[F]): Time[F] = time

  implicit def instance[F[_]: Clock: Functor]: Time[F] = new Time[F] {
    override def timestamp: F[Timestamp] =
      F.realTime(TimeUnit.MILLISECONDS)
        .map(Instant.ofEpochMilli)
        .map(Timestamp(_))
  }
}
