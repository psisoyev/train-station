package com.psisoyev.train.station

import cats.effect.kernel.Async
import cats.implicits._
import ciris._
import com.comcast.ip4s.Port
import cr.pulsar.Pulsar.PulsarURL
import io.estatico.newtype.Coercible
import io.estatico.newtype.ops._

case class Config(
  pulsarUrl: PulsarURL,
  httpPort: Port,
  city: City,
  connectedTo: List[City]
)

object Config {
  implicit def coercibleDecoder[A: Coercible[B, *], B: ConfigDecoder[String, *]]: ConfigDecoder[String, A] =
    ConfigDecoder[String, B].map(_.coerce[A])

  implicit def listDecoder[A: ConfigDecoder[String, *]]: ConfigDecoder[String, List[A]] =
    ConfigDecoder.lift(_.split(",").map(_.trim).toList.traverse(A.decode(None, _)))

  implicit class ConfigOps[F[_], A](cv: ConfigValue[F, A]) {
    // Same as `default` but it allows you to use the underlying type of the newtype
    def withDefault[T](value: T)(implicit ev: Coercible[T, A]): ConfigValue[F, A] =
      cv.default(value.coerce[A])
  }

  implicit final val stringComcastPortDecoder: ConfigDecoder[String, Port] =
    ConfigDecoder[String].mapOption("com.comcast.ip4s.Port")(Port.fromString)

  private def value[F[_]]: ConfigValue[F, Config] =
    (
      env("PULSAR_SERVICE_URL").as[PulsarURL].withDefault("pulsar://localhost:6650"),
      env("HTTP_PORT").as[Port],
      env("CITY").as[City],
      env("CONNECTED_TO").as[List[City]]
    ).parMapN(Config.apply)

  def load[F[_]: Async]: F[Config] = value[F].load
}
