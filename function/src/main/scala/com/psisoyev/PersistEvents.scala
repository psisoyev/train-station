package com.psisoyev

import java.nio.charset.StandardCharsets.UTF_8

import cats.effect.{ Blocker, ContextShift, IO }
import cats.implicits._
import com.psisoyev.PersistEvents._
import cr.pulsar.{ Record, WindowContext, WindowFunction }
import doobie.implicits._
import doobie.util.transactor.Transactor.Aux
import doobie.{ Transactor, Update }
import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder
import io.circe.parser.parse

import scala.concurrent.ExecutionContext.Implicits.global

class PersistEvents extends WindowFunction[Array[Byte], Int] {
  override def handle(input: Seq[Record[Array[Byte]]], context: WindowContext): Int = {
    val xa = transactor(context)

    val data =
      input.map { record =>
        val raw = new String(record.value, UTF_8)

        parse(raw)
          .flatMap(_.as[RawEvent])
          .map(e => (e.id, e.event.created, e.event.`type`, raw))
      }.collect { case Right(t) => t }.toList

    Update[Event](sql)
      .updateMany(data)
      .transact(xa)
      .unsafeRunSync()

    data.size
  }
}

object PersistEvents {
  implicit val cs: ContextShift[IO] = IO.contextShift(global)

  final case class EventMetadata(created: Long, `type`: String)
  final case class RawEvent(id: String, event: EventMetadata)
  type Event = (String, Long, String, String)

  implicit val eventMetadataDecoder: Decoder[EventMetadata] = deriveDecoder
  implicit val rawEventDecoder: Decoder[RawEvent]           = deriveDecoder

  def getConfig(context: WindowContext, key: String, default: String): String =
    context.userConfigValueOrElse(key, default)

  def transactor(context: WindowContext)(implicit cs: ContextShift[IO]): Aux[IO, Unit] = {
    val dbUrl = getConfig(context, "dbUrl", "jdbc:postgresql://localhost:5432/postgres")
    val user  = getConfig(context, "dbUser", "postgres")
    val pass  = getConfig(context, "dbPass", "mysecretpassword")

    Transactor.fromDriverManager[IO](
      "org.postgresql.Driver",
      dbUrl,
      user,
      pass,
      Blocker.liftExecutionContext(global)
    )
  }

  val sql: String = "insert into event (analytics_id, created, event_type, raw) values (?, ?, ?, ?)"
}
