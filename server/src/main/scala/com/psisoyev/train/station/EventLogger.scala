package com.psisoyev.train.station

import cr.pulsar.Topic.URL
import cr.pulsar.{ Logger => NeutronLogger }
import io.circe.Encoder
import io.circe.syntax._

object EventLogger {
  private def logEvent[F[_]: Logger, E: Encoder](prefix: String)(topic: URL, event: E): F[Unit] =
    F.info(s"[$prefix] [$topic] ${event.asJson.noSpaces}")

  def incomingEvents[F[_]: Logger, E: Encoder]: NeutronLogger[F, E] =
    (topic: URL, e: E) => logEvent("Incoming")(topic, e)

  def outgoingEvents[F[_]: Logger, E: Encoder]: NeutronLogger[F, E] =
    (topic: URL, e: E) => logEvent("Outgoing")(topic, e)
}
