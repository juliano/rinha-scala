package io.github.juliano.rinha

import zio.*
import zio.http.*
import zio.json.*
import zio.redis.Redis

import java.util.UUID

import exceptions.*

object Routes:
  def apply(): Http[Repository & Redis, Response, Request, Response] =
    Http.collectZIO[Request] {
      case request @ (Method.POST -> Root / "pessoas") =>
        (for
          input  <- request.body.asString.map(_.fromJson[Input])
          req    <- ZIO.fromEither(input).mapError(WrongFormatException.apply)
          pessoa <- Pessoa.make(req)
          id     <- Repository.save(pessoa)
          r       = Response.status(Status.Created).addHeader("Location", s"/pessoas/$id")
        yield r).mapError {
          case m: MissingFieldException => Response.status(Status.UnprocessableEntity)
          case r: RegisterAlreadyExists => Response.status(Status.UnprocessableEntity)
          case w: WrongFormatException  => Response.status(Status.BadRequest)
          case t: Throwable             => Response.status(Status.InternalServerError)
        }

      case Method.GET -> Root / "pessoas" / id =>
        for
          uuid <- ZIO.attempt(UUID.fromString(id)).orElseFail(Response.status(Status.NotFound))
          resp <- Repository
                    .findById(uuid)
                    .fold(
                      _ => Response.status(Status.InternalServerError),
                      {
                        case Some(pessoa) => Response.json(Output(pessoa).toJson)
                        case None         => Response.status(Status.NotFound)
                      }
                    )
        yield resp

      case request @ (Method.GET -> Root / "pessoas") =>
        val term = request.url.queryParams.get("t")
        term match
          case None =>
            ZIO.succeed(Response.status(Status.BadRequest))
          case Some(value) =>
            Repository
              .search(value.asString)
              .fold(
                e => Response.status(Status.InternalServerError),
                list => Response.json(list.map(Output(_)).toJson)
              )

      case Method.GET -> Root / "contagem-pessoas" =>
        Repository.count.fold(
          _ => Response.status(Status.InternalServerError),
          n => Response.text(n.toString)
        )
    }
