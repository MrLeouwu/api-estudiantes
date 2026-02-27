package main

import cats.effect.{IO, IOApp, ExitCode}

import doobie._
import doobie.implicits._
import doobie.hikari._

import sttp.tapir._
import sttp.tapir.generic.auto._
import sttp.tapir.json.circe._
import sttp.tapir.server.http4s._
import sttp.tapir.swagger.bundle.SwaggerInterpreter

import io.circe.generic.auto._

import org.http4s._
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.Router
import org.http4s.dsl.io._
import org.http4s.implicits._

import com.comcast.ip4s._
import scala.concurrent.ExecutionContext

import models.{Estudiante, LoginRequest, LoginResponse}
import repository.{EstudianteRepository, UsuarioRepository}
import service.EstudianteService

object Main extends IOApp {

  override def run(args: List[String]): IO[ExitCode] = {

    val dbUrl  = sys.env.getOrElse("DB_URL", sys.env.getOrElse("DATABASE_URL", "jdbc:mysql://localhost:3306/estudiantes_db"))
    val dbUser = sys.env.getOrElse("DB_USER", "root")
    val dbPass = sys.env.getOrElse("DB_PASS", "")

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

      val usuarioRepo = new UsuarioRepository(xa)
      val estudianteRepo = new EstudianteRepository(xa)
      val estudianteService = new EstudianteService(estudianteRepo)

      val baseEndpoint = endpoint.in("estudiantes")

      val getAll =
        baseEndpoint.get
          .out(jsonBody[List[Estudiante]])
          .serverLogic[IO](_ =>
            estudianteService.getAll.map(Right(_))
          )

      val getById =
        baseEndpoint.get
          .in(path[Int]("id"))
          .out(jsonBody[Option[Estudiante]])
          .serverLogic[IO](id =>
            estudianteService.getById(id).map(Right(_))
          )

      val create =
        baseEndpoint.post
          .in(jsonBody[Estudiante])
          .out(jsonBody[Int])
          .serverLogic[IO](est =>
            estudianteService.create(est).map(Right(_))
          )

      val delete =
        baseEndpoint.delete
          .in(path[Int]("id"))
          .out(jsonBody[Int])
          .serverLogic[IO](id =>
            estudianteService.delete(id).map(Right(_))
          )

      val loginEndpoint = endpoint.in("auth").in("login")
      val loginRoute =
        loginEndpoint.post
          .in(jsonBody[LoginRequest])
          .out(jsonBody[LoginResponse])
          .serverLogic[IO](req =>
            usuarioRepo.findByUsername(req.username).map {
              case Some(usuario) if usuario.password == req.password =>
                val token = usuario.apiToken.getOrElse("")
                Right(LoginResponse(token, "Login exitoso"))
              case _ =>
                Left("Usuario o contraseña incorrectos")
            }
          )

      val registerEndpoint = endpoint.in("auth").in("register")
      val registerRoute =
        registerEndpoint.post
          .in(jsonBody[LoginRequest])
          .out(jsonBody[String])
          .serverLogic[IO](req =>
            usuarioRepo.register(req.username, req.password).map { _ =>
              Right("Usuario registrado. Ahora puedes hacer login.")
            }.handleErrorWith(e => IO(Left(s"Error: ${e.getMessage}")))
          )

      val swaggerEndpoints =
        SwaggerInterpreter()
          .fromServerEndpoints[IO](
            List(getAll, getById, create, delete, loginRoute, registerRoute),
            "API Estudiantes",
            "1.0"
          )

      val allRoutes = Http4sServerInterpreter[IO]().toRoutes(
        List(getAll, getById, create, delete, loginRoute, registerRoute) ++ swaggerEndpoints
      )

      val httpApp: HttpApp[IO] = Router(
        "/" -> allRoutes
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
