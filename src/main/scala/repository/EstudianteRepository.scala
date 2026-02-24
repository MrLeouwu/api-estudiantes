package repository

import cats.effect._
import doobie._
import doobie.implicits._
import models.Estudiante

class EstudianteRepository(xa: Transactor[IO]) {

  def findAll: IO[List[Estudiante]] =
    sql"SELECT id, nombre, edad FROM estudiantes"
      .query[Estudiante]
      .to[List]
      .transact(xa)

  def findById(id: Int): IO[Option[Estudiante]] =
    sql"SELECT id, nombre, edad FROM estudiantes WHERE id = $id"
      .query[Estudiante]
      .option
      .transact(xa)

  def create(est: Estudiante): IO[Int] =
    sql"""
        INSERT INTO estudiantes (nombre, edad)
        VALUES (${est.nombre}, ${est.edad})
      """
      .update
      .withUniqueGeneratedKeys[Int]("id")
      .transact(xa)

  def delete(id: Int): IO[Int] =
    sql"DELETE FROM estudiantes WHERE id = $id"
      .update
      .run
      .transact(xa)
}