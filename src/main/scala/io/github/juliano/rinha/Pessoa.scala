package io.github.juliano.rinha

import zio.*
import zio.prelude.Assertion.*
import zio.prelude.{Subtype, Validation}

import java.time.LocalDate
import java.util.UUID

import exceptions.*
import Pessoa.*
import zio.schema.Schema
import zio.schema.DeriveSchema

final case class Pessoa(
  id: UUID,
  apelido: String,
  nome: String,
  nascimento: LocalDate,
  stack: Option[Chunk[String]]
)

object Pessoa:
  given Schema[Pessoa] = DeriveSchema.gen

  case class MandatoryFields(apelido: String, nome: String, nascimento: String)

  object MandatoryFields:
    import Validation.fromOptionWith

    def make(in: Input): Validation[MissingFieldException, MandatoryFields] =
      Validation
        .validateWith(
          fromOptionWith("apelido")(in.apelido),
          fromOptionWith("nome")(in.nome),
          fromOptionWith("nascimento")(in.nascimento)
        )(MandatoryFields.apply)
        .mapError(MissingFieldException.apply)

  def make(in: Input): IO[IllegalArgumentException, Pessoa] =
    for
      id      <- Random.nextUUID
      fields  <- MandatoryFields.make(in).toZIO
      apelido <- Apelido.make(fields.apelido).toZIO.mapError(WrongFormatException.apply)
      nome    <- Nome.make(fields.nome).toZIO.mapError(WrongFormatException.apply)
      date    <- ZIO.attempt(LocalDate.parse(fields.nascimento)).mapError(t => WrongFormatException(t.getMessage))
    yield Pessoa(id, apelido, nome, date, in.stack)

  type Apelido = Apelido.Type
  object Apelido extends RichSubtype[String]:
    override inline def assertion = hasLength(between(1, 32))

  type Nome = Nome.Type
  object Nome extends RichSubtype[String]:
    override inline def assertion = hasLength(between(1, 100))

  type Nascimento = Nascimento.Type
  object Nascimento extends RichSubtype[String]:
    override inline def assertion = matches("^\\d{4}-\\d{2}-\\d{2}$".r)

  type ItemStack = ItemStack.Type
  object ItemStack extends RichSubtype[String]:
    override inline def assertion = hasLength(between(1, 32))
