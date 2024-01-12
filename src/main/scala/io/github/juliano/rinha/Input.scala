package io.github.juliano.rinha

import zio.Chunk
import zio.json.{DeriveJsonDecoder, JsonDecoder}

final case class Input(
  apelido: Option[String],
  nome: Option[String],
  nascimento: Option[String],
  stack: Option[Chunk[String]]
)

object Input:
  given JsonDecoder[Input] = DeriveJsonDecoder.gen
