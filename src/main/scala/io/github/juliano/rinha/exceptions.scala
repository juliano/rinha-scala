package io.github.juliano.rinha

import java.sql.SQLException

object exceptions:
  case class MissingFieldException(f: String) extends IllegalArgumentException(s"Field $f is missing")
  case class WrongFormatException(s: String) extends IllegalArgumentException(s"Wrong format: $s")
  case class RegisterAlreadyExists(t: SQLException) extends SQLException(t.getMessage)
  object RegisterAlreadyExists:
    def apply(s: String): RegisterAlreadyExists = RegisterAlreadyExists(new SQLException(s))