package com.psisoyev

import java.nio.charset.StandardCharsets.UTF_8

import cats.effect.{ ContextShift, IO }
import cats.implicits._
import com.psisoyev.PersistEvents._
import cr.pulsar.{ Record, WindowContext, WindowFunction }
import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder
import io.circe.parser.parse
import skunk._
import skunk.codec.all._
import skunk.implicits._
import natchez.Trace.Implicits.noop

import scala.concurrent.ExecutionContext.Implicits.global

class PersistEvents extends WindowFunction[Array[Byte], Int] {
  val pool = Session
    .pooled[IO](
      host = "localhost",
      port = 5432,
      database = "postgres",
      user = "postgres",
      password = Some("mysecretpassword"),
      max = 1
    )

  override def handle(input: Seq[Record[Array[Byte]]], context: WindowContext): Int = {
    val data =
      input.map { record =>
        val raw = new String(record.value, UTF_8)

        parse(raw)
          .flatMap(_.as[RawEvent])
          .map(e => AnalyticalEvent(e.id, e.event.created, e.event.`type`, raw))
      }.collect { case Right(t) => t }.toList

    pool.flatten.use { session =>
      session.prepare(sql).use { cmd =>
        data.traverse(cmd.execute)
      }
    }

    data.size
  }
}

object PersistEvents {
  implicit val cs: ContextShift[IO] = IO.contextShift(global)

  final case class EventMetadata(created: Long, `type`: String)
  final case class RawEvent(id: String, event: EventMetadata)

  final case class AnalyticalEvent(id: String, created: Long, name: String, raw: String)

  implicit val eventMetadataDecoder: Decoder[EventMetadata] = deriveDecoder
  implicit val rawEventDecoder: Decoder[RawEvent]           = deriveDecoder

  val encoder: Encoder[AnalyticalEvent] =
    (
      varchar ~ int8 ~ varchar ~ varchar
    ).contramap[AnalyticalEvent] { b =>
      b.id ~ b.created ~ b.name ~ b.raw
    }

  val sql: Command[AnalyticalEvent] =
    sql"""
        INSERT INTO event
        VALUES ($encoder)
        ON CONFLICT DO NOTHING
        """.command
}
