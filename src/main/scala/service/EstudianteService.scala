package service

import cats.effect._
import models._
import repository._

class EstudianteService(repo: EstudianteRepository) {

  def getAll: IO[List[Estudiante]] =
    repo.findAll

  def getById(id: Int): IO[Option[Estudiante]] =
    repo.findById(id)

  def create(est: Estudiante): IO[Int] =
    repo.create(est)

  def delete(id: Int): IO[Int] =
    repo.delete(id)

  def update(est: Estudiante): IO[Int] =
    repo.update(est)
}