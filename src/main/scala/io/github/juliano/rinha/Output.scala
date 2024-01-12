package io.github.juliano.rinha

import zio.Chunk
import zio.json.{DeriveJsonEncoder, JsonEncoder}
import zio.json.ast.Json
import zio.json.internal.Write

import java.util.UUID

final case class Output(
  id: UUID,
  apelido: String,
  nome: String,
  nascimento: String,
  stack: Option[Chunk[String]]
)

object Output:
  def apply(p: Pessoa): Output =
    Output(p.id, p.apelido, p.nome, p.nascimento.toString, p.stack)

  given JsonEncoder[Output] = DeriveJsonEncoder.gen[Output]

  given noneAsNull[A](using JsonEncoder[A]): JsonEncoder[Option[A]] = new JsonEncoder[Option[A]]:
    val optionEncoder = JsonEncoder.option
    override def unsafeEncode(oa: Option[A], indent: Option[Int], out: Write): Unit =
      optionEncoder.unsafeEncode(oa, indent, out)

    override def isNothing(oa: Option[A]): Boolean =
      oa match
        case None    => false
        case Some(a) => optionEncoder.isNothing(oa)

    override final def toJsonAST(oa: Option[A]): Either[String, Json] =
      optionEncoder.toJsonAST(oa)