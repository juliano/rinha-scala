package io.github.juliano.rinha

import zio.*
import zio.http.*
import zio.redis.*
import zio.schema.Schema
import zio.schema.codec.{BinaryCodec, JsonCodec}

object Main extends ZIOAppDefault:
  object JsonCodecSupplier extends CodecSupplier:
    def get[A: Schema]: BinaryCodec[A] = JsonCodec.schemaBasedBinaryCodec

  def run =
    for
      port <- System.env("HTTP_PORT").someOrElse("9999").map(_.toInt)
      redisHost <- System.env("REDIS_HOST").someOrElse("redis")
      _ <- Server
             .serve(Routes())
             .provide(
               Server.defaultWithPort(port),
               Repository.layer,
               Redis.layer,
               RedisExecutor.layer,
               ZLayer.succeed(RedisConfig(redisHost, 6379)),
               ZLayer.succeed[CodecSupplier](JsonCodecSupplier)
             )
    yield ()
