package repository

import cats.effect.IO
import doobie._
import doobie.implicits._
import models.Usuario
import java.util.UUID

class UsuarioRepository(xa: Transactor[IO]) {

  def findAll: IO[List[Usuario]] =
    sql"SELECT id, username, password, api_token FROM usuarios"
      .query[Usuario]
      .to[List]
      .transact(xa)

  def findByUsername(username: String): IO[Option[Usuario]] =
    sql"SELECT id, username, password, api_token FROM usuarios WHERE username = $username"
      .query[Usuario]
      .option
      .transact(xa)

  def findById(id: Int): IO[Option[Usuario]] =
    sql"SELECT id, username, password, api_token FROM usuarios WHERE id = $id"
      .query[Usuario]
      .option
      .transact(xa)

  def findByToken(token: String): IO[Option[Usuario]] =
    sql"SELECT id, username, password, api_token FROM usuarios WHERE api_token = $token"
      .query[Usuario]
      .option
      .transact(xa)

  def create(username: String, password: String): IO[Option[Int]] =
    sql"""
      INSERT INTO usuarios (username, password) VALUES ($username, $password)
    """.update
      .withUniqueGeneratedKeys[Int]("id")
      .transact(xa)
      .map(Some(_))
      .handleErrorWith(_ => IO(None))

  def delete(id: Int): IO[Int] =
    sql"DELETE FROM usuarios WHERE id = $id"
      .update.run
      .transact(xa)

  def updateToken(id: Int, token: String): IO[Int] = {
    sql"UPDATE usuarios SET api_token = $token WHERE id = $id"
      .update.run
      .transact(xa)
  }

  def updateFull(id: Int, username: String, password: String): IO[Int] =
    sql"UPDATE usuarios SET username = $username, password = $password WHERE id = $id"
      .update.run
      .transact(xa)
}
