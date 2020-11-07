package com.psisoyev.train.station

import java.time.format.DateTimeFormatter
import cats.Functor
import cats.implicits._

trait Logger[F[_]] {
  def info(s: => String): F[Unit]
  def error(s: => String): F[Unit]
}

/**
 * Naive implementation of a logger, not for production use
 */
object Logger {
  def apply[F[_]](implicit logger: Logger[F]): Logger[F] = logger

  implicit def instance[F[_]: Functor: Time]: Logger[F] = new Logger[F] {
    def print(s: => String): F[Unit] =
      F.timestamp.map { ts =>
        val timestamp = DateTimeFormatter.ISO_INSTANT.format(ts.value)
        println(s"[$timestamp] $s")
      }

    def info(s: => String): F[Unit]  = print(s)
    def error(s: => String): F[Unit] = print(s)
  }
}
