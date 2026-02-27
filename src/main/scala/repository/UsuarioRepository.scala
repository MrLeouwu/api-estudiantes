package repository

import cats.effect.IO
import doobie._
import doobie.implicits._
import models.Usuario
import java.util.UUID

class UsuarioRepository(xa: Transactor[IO]) {

  def findByUsername(username: String): IO[Option[Usuario]] =
    sql"SELECT id, username, password, api_token FROM usuarios WHERE username = $username"
      .query[Usuario]
      .option
      .transact(xa)

  def findByToken(token: String): IO[Option[Usuario]] =
    sql"SELECT id, username, password, api_token FROM usuarios WHERE api_token = $token"
      .query[Usuario]
      .option
      .transact(xa)

  def createToken(username: String, password: String): IO[Option[String]] = {
    val token = UUID.randomUUID().toString
    sql"""
      UPDATE usuarios 
      SET api_token = $token 
      WHERE username = $username AND password = $password
    """.update.run
      .transact(xa)
      .map { rows => 
        if (rows > 0) Some(token) else None
      }
  }

  def register(username: String, password: String): IO[Int] = {
    sql"""
      INSERT INTO usuarios (username, password) VALUES ($username, $password)
    """.update.run
      .transact(xa)
  }
}
