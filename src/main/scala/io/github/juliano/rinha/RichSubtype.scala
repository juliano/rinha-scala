package io.github.juliano.rinha

import io.getquill.MappedEncoding
import zio.json.JsonDecoder
import zio.prelude.Subtype

trait RichSubtype[A] extends Subtype[A]:
  given (using d: JsonDecoder[A]): JsonDecoder[Type] =
    d.mapOrFail(make(_).toEitherAssociative)

  given MappedEncoding[A, Type] = MappedEncoding(wrap)
  given MappedEncoding[Type, A] = MappedEncoding(unwrap)