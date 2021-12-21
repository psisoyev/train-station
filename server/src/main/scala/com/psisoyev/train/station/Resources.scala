package com.psisoyev.train.station

import cats.effect.kernel.Async
import cats.effect.{ Ref, Resource }
import cats.implicits._
import cats.{ Inject, Parallel }
import com.psisoyev.train.station.arrival.ExpectedTrains.ExpectedTrain
import cr.pulsar.Producer.Options
import cr.pulsar.internal.FutureLift._
import cr.pulsar.{ Consumer, Producer, Pulsar, Subscription, Topic, Config => PulsarConfig }
import io.circe.{ Decoder, Encoder }
import io.circe._
import io.circe.parser.decode
import io.circe.syntax._

import java.nio.charset.StandardCharsets.UTF_8

final case class Resources[F[_], E](
  config: Config,
  producer: Producer[F, E],
  consumers: List[Consumer[F, E]],
  trainRef: Ref[F, Map[TrainId, ExpectedTrain]]
)

object Resources {
  implicit def circeBytesInject[T: Encoder: Decoder]: Inject[T, Array[Byte]] =
    new Inject[T, Array[Byte]] {
      val inj: T => Array[Byte] =
        _.asJson.noSpaces.getBytes(UTF_8)

      val prj: Array[Byte] => Option[T] =
        bytes => decode[T](new String(bytes, UTF_8)).toOption
    }

  def make[
    F[_]: Async: Logger: Parallel,
    E: Decoder: Encoder
  ]: Resource[F, Resources[F, E]] = {
    def topic(config: PulsarConfig, city: City) =
      Topic
        .Builder
        .withName(Topic.Name(city.value.toLowerCase))
        .withConfig(config)
        .withType(Topic.Type.Persistent)
        .build

    def consumer(client: Pulsar.T, config: Config, city: City): Resource[F, Consumer[F, E]] = {
      val name         = s"${city.value}-${config.city.value}"
      val subscription =
        Subscription
          .Builder
          .withName(Subscription.Name(name))
          .withType(Subscription.Type.Failover)
          .build

      val options =
        Consumer
          .Options[F, E]()
          .withLogger(EventLogger.incomingEvents)

      Consumer.make[F, E](client, topic(config.pulsar, city), subscription, options)
    }

    def producer(client: Pulsar.T, config: Config): Resource[F, Producer[F, E]] = {
      val opts = Options[F, E]().withLogger(EventLogger.outgoingEvents)
      Producer.make[F, E](client, topic(config.pulsar, config.city), opts)
    }

    for {
      config    <- Resource.eval(Config.load[F])
      client    <- Pulsar.make[F](config.pulsar.url)
      producer  <- producer(client, config)
      consumers <- config.connectedTo.traverse(consumer(client, config, _))
      trainRef  <- Resource.eval(Ref.of[F, Map[TrainId, ExpectedTrain]](Map.empty))
    } yield Resources(config, producer, consumers, trainRef)
  }
}
