package com.psisoyev.train.station.arrival

import cats.Functor
import cats.implicits._
import com.psisoyev.train.station.{ Expected, From, TrainId }
import com.psisoyev.train.station.arrival.ExpectedTrains.ExpectedTrain
import cats.effect.Ref

trait ExpectedTrains[F[_]] {
  def get(id: TrainId): F[Option[ExpectedTrain]]
  def remove(id: TrainId): F[Unit]
  def update(id: TrainId, expectedTrain: ExpectedTrain): F[Unit]
}

object ExpectedTrains {
  case class ExpectedTrain(from: From, time: Expected)

  def make[F[_]: Functor](
    ref: Ref[F, Map[TrainId, ExpectedTrain]]
  ): ExpectedTrains[F] = new ExpectedTrains[F] {
    override def get(id: TrainId): F[Option[ExpectedTrain]]         = ref.get.map(_.get(id))
    override def remove(id: TrainId): F[Unit]                       = ref.update(_.removed(id))
    override def update(id: TrainId, train: ExpectedTrain): F[Unit] = ref.update(_.updated(id, train))
  }
}
