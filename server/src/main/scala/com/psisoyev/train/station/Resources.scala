package com.psisoyev.train.station

import cats.effect.concurrent.Ref
import cats.effect.{ Concurrent, ContextShift, Resource }
import cats.implicits._
import cats.{ Inject, Parallel }
import com.psisoyev.train.station.arrival.ExpectedTrains.ExpectedTrain
import cr.pulsar.{ Consumer, Producer, Pulsar, Subscription, Topic, Config => PulsarConfig }
import io.circe.Encoder

final case class Resources[F[_], E](
  config: Config,
  producer: Producer[F, E],
  consumers: List[Consumer[F, E]],
  trainRef: Ref[F, Map[TrainId, ExpectedTrain]]
)

object Resources {
  def make[
    F[_]: Concurrent: ContextShift: Parallel: Logger,
    E: Inject[*, Array[Byte]]: Encoder
  ]: Resource[F, Resources[F, E]] = {
    def topic(config: PulsarConfig, city: City) =
      Topic(
        Topic.Name(city.value.toLowerCase),
        config
      ).withType(Topic.Type.Persistent)

    def consumer(client: Pulsar.T, config: Config, city: City): Resource[F, Consumer[F, E]] = {
      val name = s"${city.value}-${config.city.value}"
      val subscription =
        Subscription(Subscription.Name(name))
          .withType(Subscription.Type.Failover)
      val options =
        Consumer
          .Options[F, E]()
          .withLogger(EventLogger.incomingEvents)

      Consumer.withOptions[F, E](client, topic(config.pulsar, city), subscription, options)
    }

    def producer(client: Pulsar.T, config: Config): Resource[F, Producer[F, E]] =
      Producer.withLogger[F, E](client, topic(config.pulsar, config.city), EventLogger.outgoingEvents)

    for {
      config    <- Resource.liftF(Config.load[F])
      client    <- Pulsar.create[F](config.pulsar.serviceUrl)
      producer  <- producer(client, config)
      consumers <- config.connectedTo.traverse(consumer(client, config, _))
      trainRef  <- Resource.liftF(Ref.of[F, Map[TrainId, ExpectedTrain]](Map.empty))
    } yield Resources(config, producer, consumers, trainRef)
  }
}
