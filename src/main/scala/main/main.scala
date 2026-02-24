package main

import cats.effect._
import cats.effect.IO

import doobie._
import doobie.implicits._
import doobie.hikari._

import sttp.tapir._
import sttp.tapir.generic.auto._
import sttp.tapir.json.circe._
import sttp.tapir.server.http4s._
import sttp.tapir.swagger.bundle.SwaggerInterpreter

import io.circe.generic.auto._

import org.http4s.HttpApp
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.Router

import com.comcast.ip4s._

import scala.concurrent.ExecutionContext

import models.Estudiante
import repository.EstudianteRepository
import service.EstudianteService

object Main extends IOApp {

  override def run(args: List[String]): IO[ExitCode] = {

    // 🔹 Leer variables de entorno (Docker o Local)
    val dbUrl  = sys.env.getOrElse("DB_URL", "jdbc:mysql://localhost:3306/estudiantes_db")
    val dbUser = sys.env.getOrElse("DB_USER", "root")
    val dbPass = sys.env.getOrElse("DB_PASS", "")

    // 🔹 ExecutionContext para Doobie
    val connectEC = ExecutionContext.global

    val transactorResource =
      HikariTransactor.newHikariTransactor[IO](
        driverClassName = "com.mysql.cj.jdbc.Driver",
        url = dbUrl,
        user = dbUser,
        pass = dbPass,
        connectEC = connectEC
      )

    transactorResource.use { xa =>

      val repository = new EstudianteRepository(xa)
      val service = new EstudianteService(repository)

      val baseEndpoint = endpoint.in("estudiantes")

      val getAll =
        baseEndpoint.get
          .out(jsonBody[List[Estudiante]])
          .serverLogic[IO](_ =>
            service.getAll.map(Right(_))
          )

      val getById =
        baseEndpoint.get
          .in(path[Int]("id"))
          .out(jsonBody[Option[Estudiante]])
          .serverLogic[IO](id =>
            service.getById(id).map(Right(_))
          )

      val create =
        baseEndpoint.post
          .in(jsonBody[Estudiante])
          .out(jsonBody[Int])
          .serverLogic[IO](est =>
            service.create(est).map(Right(_))
          )

      val delete =
        baseEndpoint.delete
          .in(path[Int]("id"))
          .out(jsonBody[Int])
          .serverLogic[IO](id =>
            service.delete(id).map(Right(_))
          )

      val swaggerEndpoints =
        SwaggerInterpreter()
          .fromServerEndpoints[IO](
            List(getAll, getById, create, delete),
            "API Estudiantes",
            "1.0"
          )

      val httpApp: HttpApp[IO] =
        Router(
          "/" -> Http4sServerInterpreter[IO]().toRoutes(
            List(getAll, getById, create, delete) ++ swaggerEndpoints
          )
        ).orNotFound

      EmberServerBuilder.default[IO]
        .withHost(host"0.0.0.0")
        .withPort(port"8080")
        .withHttpApp(httpApp)
        .build
        .useForever
    }.as(ExitCode.Success)
  }
}