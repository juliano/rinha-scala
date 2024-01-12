package io.github.juliano.rinha

import io.getquill.*
import io.getquill.jdbczio.Quill
import zio.*
import zio.redis.Redis

import java.sql.SQLException
import java.time.LocalDate
import java.util.UUID
import javax.sql.DataSource

import exceptions.RegisterAlreadyExists
import Pessoa.*
import Repository.*

final case class Repository(ds: DataSource, queue: Queue[Pessoa], redis: Redis):
  val ctx = new PostgresZioJdbcContext(SnakeCase)
  import ctx.*

  def save(pessoa: Pessoa): Task[UUID] =
    for
      opt <- redis.get(pessoa.apelido).returning[UUID]
      id <- opt match
              case Some(uuid) => ZIO.fail(RegisterAlreadyExists(uuid.toString))
              case None =>
                for
                  _ <- redis.set(pessoa.apelido, pessoa.id)
                  _ <- redis.set(pessoa.id, pessoa)
                  _ <- queue.offer(pessoa)
                  _ <- save.fork
                yield pessoa.id
    yield id

  private def save: Task[Unit] =
    for
      pessoa <- queue.take
      _      <- persist(pessoa)
    yield ()

  def persist(p: Pessoa): IO[SQLException, Unit] =
    run(query[Table].insertValue(lift(Table.fromModel(p)))).unit.mapError {
      case e: SQLException if e.getMessage.contains("violates unique constraint \"pessoas_apelido_key\"") =>
        RegisterAlreadyExists(e)
    }
      .provide(ZLayer.succeed(ds))

  def findById(id: UUID): Task[Option[Pessoa]] =
    for
      opt <- redis.get(id).returning[Pessoa]
      result <- opt match
                  case s @ Some(p) => ZIO.succeed(s)
                  case None =>
                    for
                      opt <- queryById(id)
                      res <- opt match
                               case None            => ZIO.none
                               case s @ Some(value) => redis.set(id, value).as(s)
                    yield res
    yield result

  def queryById(id: UUID): IO[SQLException, Option[Pessoa]] =
    run(query[Table].filter(_.id == lift(id))).head
      .map(Table.toModel)
      .unsome
      .provide(ZLayer.succeed(ds))

  def search(term: String): Task[Chunk[Pessoa]] =
    for
      opt <- redis.get(term).returning[Chunk[Pessoa]]
      result <- opt match
                  case Some(pessoas) => ZIO.succeed(pessoas)
                  case None =>
                    for
                      res <- queryByTerm(term)
                      _   <- redis.set(term, res)
                    yield res
    yield result

  def queryByTerm(term: String): IO[SQLException, Chunk[Pessoa]] =
    (for
      items <- run(query[Table].filter(_.termoBusca like lift(s"%${term.toLowerCase}%")).take(50))
      result = Chunk.fromIterable(items.map(Table.toModel))
    yield result).provide(ZLayer.succeed(ds))

  def count: IO[SQLException, Long] = run(query[Table].size).provide(ZLayer.succeed(ds))

object Repository:
  case class Table(
    id: UUID,
    apelido: String,
    nome: String,
    nascimento: LocalDate,
    stack: Option[String],
    termoBusca: String
  )

  object Table:
    inline given SchemaMeta[Table] = schemaMeta("pessoas")
    inline given InsertMeta[Table] = insertMeta(_.termoBusca)

    def fromModel(p: Pessoa): Table =
      Table(p.id, p.apelido, p.nome, p.nascimento, p.stack.map(_.mkString(";")), null)

    def toModel(t: Table): Pessoa =
      Pessoa(t.id, t.apelido, t.nome, t.nascimento, t.stack.map(s => Chunk.fromArray(s.split(";"))))

  val layer: RLayer[Redis, Repository] =
    (Quill.DataSource.fromPrefix("db") ++ ZLayer.fromZIO(Queue.unbounded[Pessoa])) >>>
      ZLayer.fromFunction(Repository.apply)

  def save(p: Pessoa): RIO[Repository, UUID] =
    ZIO.serviceWithZIO(_.save(p))

  def findById(id: UUID): RIO[Repository, Option[Pessoa]] =
    ZIO.serviceWithZIO(_.findById(id))

  def search(term: String): RIO[Repository, Chunk[Pessoa]] =
    ZIO.serviceWithZIO(_.search(term))

  def count: RIO[Repository, Long] = ZIO.serviceWithZIO(_.count)
