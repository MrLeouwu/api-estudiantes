package service

import cats.effect._
import models._
import repository._

class UsuarioService(repo: UsuarioRepository) {

  def getAll: IO[List[Usuario]] =
    repo.findAll

  def getById(id: Int): IO[Option[Usuario]] =
    repo.findById(id)

  def create(usuario: Usuario): IO[Option[Int]] =
    repo.create(usuario.username, usuario.password)

  def delete(id: Int): IO[Int] =
    repo.delete(id)

  def update(usuario: Usuario): IO[Int] =
    usuario.id match {
      case Some(id) => repo.updateFull(id, usuario.username, usuario.password)
      case None => IO.pure(0)
    }
}
