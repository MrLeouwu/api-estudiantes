package database

import cats.effect._
import cats.effect.std.Console
import doobie._
import doobie.hikari._
import doobie.implicits._

object Database {

  val transactor: Resource[IO, HikariTransactor[IO]] =
    for {
      connectEC <- ExecutionContexts.fixedThreadPool[IO](32)
      xa <- HikariTransactor.newHikariTransactor[IO](
        driverClassName = "com.mysql.cj.jdbc.Driver",
        url             = "jdbc:mysql://localhost:3306/estudiantes_db?serverTimezone=UTC",
        user            = "root",
        pass            = "",
        connectEC       = connectEC
      )
    } yield xa
}