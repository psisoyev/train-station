package com.psisoyev.train.station

import cats.effect.{ Async, ContextShift }
import cats.implicits._
import ciris._
import com.psisoyev.train.station.Config.HttpPort
import cr.pulsar.Config.{ PulsarNamespace, PulsarTenant, PulsarURL }
import cr.pulsar.{ Config => PulsarConfig }
import io.estatico.newtype.Coercible
import io.estatico.newtype.macros.newtype
import io.estatico.newtype.ops._

case class Config(
  pulsar: PulsarConfig,
  httpPort: HttpPort,
  city: City,
  connectedTo: List[City]
)

object Config {
  @newtype case class HttpPort(value: Int)

  implicit def coercibleDecoder[A: Coercible[B, *], B: ConfigDecoder[String, *]]: ConfigDecoder[String, A] =
    ConfigDecoder[String, B].map(_.coerce[A])

  implicit def listDecoder[A: ConfigDecoder[String, *]]: ConfigDecoder[String, List[A]] =
    ConfigDecoder.lift(_.split(",").map(_.trim).toList.traverse(A.decode(None, _)))

  implicit class ConfigOps[A](cv: ConfigValue[A]) {
    // Same as `default` but it allows you to use the underlying type of the newtype
    def withDefault[T](value: T)(implicit ev: Coercible[T, A]): ConfigValue[A] =
      cv.default(value.coerce[A])
  }

  def pulsarConfigValue: ConfigValue[PulsarConfig] =
    (
      env("PULSAR_TENANT").as[PulsarTenant].withDefault("public"),
      env("PULSAR_NAMESPACE").as[PulsarNamespace].withDefault("default"),
      env("PULSAR_SERVICE_URL").as[PulsarURL].withDefault("pulsar://localhost:6650")
    ).mapN { case (tenant, namespace, url) =>
      PulsarConfig
        .Builder
        .withTenant(tenant)
        .withNameSpace(namespace)
        .withURL(url)
        .build
    }

  private def value: ConfigValue[Config] =
    (
      pulsarConfigValue,
      env("HTTP_PORT").as[HttpPort].withDefault(8080),
      env("CITY").as[City],
      env("CONNECTED_TO").as[List[City]]
    ).parMapN(Config.apply)

  def load[F[_]: Async: ContextShift]: F[Config] = value.load[F]
}
