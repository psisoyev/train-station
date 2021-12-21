package com.psisoyev.train.station

import cats.Applicative
import cats.effect.{ Ref, Sync }
import cats.implicits._
import com.psisoyev.train.station.arrival.ExpectedTrains.ExpectedTrain
import cr.pulsar.{ MessageKey, Producer }
import org.apache.pulsar.client.api.MessageId
import zio.Task
import zio.test.DefaultRunnableSpec

import java.util.UUID

trait BaseSpec extends DefaultRunnableSpec {
  type F[A]           = Task[A]
  type ExpectedTrains = Map[TrainId, ExpectedTrain]

  def fakeProducer[F[_]: Sync]: F[(Ref[F, List[Event]], Producer[F, Event])] =
    Ref.of[F, List[Event]](List.empty).map { ref =>
      ref -> new Producer[F, Event] {
        override def send(msg: Event): F[MessageId]                  = ref.update(_ :+ msg).as(MessageId.latest)
        override def send_(msg: Event): F[Unit]                      = send(msg).void
        override def send(msg: Event, key: MessageKey): F[MessageId] = ???
        override def send_(msg: Event, key: MessageKey): F[Unit]     = ???
      }
    }

  val fakeUuid: UUID                                      = UUID.randomUUID()
  val eventId: EventId                                    = EventId(fakeUuid)
  implicit def fakeUuidGen[F[_]: Applicative]: UUIDGen[F] = new UUIDGen[F] {
    override def random: F[UUID]        = F.pure(fakeUuid)
    override def newEventId: F[EventId] = F.pure(eventId)
  }
}
