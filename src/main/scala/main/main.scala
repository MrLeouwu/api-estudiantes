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
import sttp.model.Header

import io.circe.generic.auto._

import org.http4s._
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.Router
import org.http4s.dsl.io._
import org.http4s.implicits._
import org.http4s.server.middleware._
import cats.implicits._

import com.comcast.ip4s._
import scala.concurrent.ExecutionContext
import java.util.UUID
import java.time.LocalDateTime

import models.{Usuario, Estudiante, LoginRequest, LoginResponse, HealthResponse}
import repository.{UsuarioRepository, EstudianteRepository}
import service.{UsuarioService, EstudianteService}

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
      val usuarioService = new UsuarioService(usuarioRepo)
      val estudianteRepo = new EstudianteRepository(xa)
      val estudianteService = new EstudianteService(estudianteRepo)

      def validateToken(token: String): IO[Either[String, String]] = {
        usuarioRepo.findByToken(token).map {
          case Some(usuario) => Right(usuario.username)
          case None => Left("Token inválido")
        }
      }

      // Health
      val healthEndpoint = endpoint.in("api").in("v1").in("health")
      val healthRoute =
        healthEndpoint.get
          .out(jsonBody[HealthResponse])
          .serverLogic[IO](_ =>
            IO(Right(HealthResponse("OK", LocalDateTime.now.toString)))
          )

      // Register usuario (público)
      val registerUsuarioEndpoint = endpoint.in("api").in("v1").in("usuarios")
      val registerUsuarioRoute =
        registerUsuarioEndpoint.post
          .in(jsonBody[LoginRequest])
          .out(jsonBody[String])
          .serverLogic[IO](req =>
            if (req.username.isEmpty || req.password.isEmpty) {
              IO(Left("Usuario y contraseña son requeridos"))
            } else {
              usuarioService.create(Usuario(None, req.username, req.password, None)).map {
                case Some(id) => Right(s"Usuario registrado. Ahora puedes hacer login.")
                case None => Left("Error al registrar usuario")
              }
            }
          )

      // Login
      val loginEndpoint = endpoint.in("api").in("v1").in("login")
      val loginRoute =
        loginEndpoint.post
          .in(jsonBody[LoginRequest])
          .out(jsonBody[LoginResponse])
          .serverLogic[IO](req =>
            if (req.username.isEmpty || req.password.isEmpty) {
              IO(Left("Usuario y contraseña son requeridos"))
            } else {
              usuarioRepo.findByUsername(req.username).flatMap {
                case Some(usuario) if usuario.password == req.password =>
                  val newToken = usuario.apiToken.getOrElse(UUID.randomUUID().toString)
                  val userId = usuario.id.getOrElse(0)
                  usuarioRepo.updateToken(userId, newToken).map { _ =>
                    Right(LoginResponse(newToken, "Login exitoso"))
                  }
                case _ =>
                  IO(Left("Usuario o contraseña incorrectos"))
              }
            }
          )

      // Estudiantes - GET all (sin token)
      val estudiantesBaseEndpoint = endpoint.in("api").in("v1").in("estudiantes")
      
      val getAllEstudiantes =
        estudiantesBaseEndpoint.get
          .out(jsonBody[List[Estudiante]])
          .serverLogic[IO](_ =>
            estudianteService.getAll.map(Right(_))
          )

      // Estudiantes - GET by ID (con token)
      val getEstudianteById =
        estudiantesBaseEndpoint.get
          .in(path[Int]("id"))
          .in(header[String]("X-Token"))
          .out(jsonBody[Option[Estudiante]])
          .serverLogic[IO] { case (id, token) =>
            validateToken(token).flatMap {
              case Right(_) => estudianteService.getById(id).map(Right(_))
              case Left(err) => IO(Left(err))
            }
          }

      // Estudiantes - POST crear (con token)
      val createEstudiante =
        estudiantesBaseEndpoint.post
          .in(jsonBody[Estudiante])
          .in(header[String]("X-Token"))
          .out(jsonBody[String])
          .serverLogic[IO] { case (estudiante, token) =>
            validateToken(token).flatMap {
              case Right(_) =>
                if (estudiante.nombre.isEmpty) {
                  IO(Left("El nombre es requerido"))
                } else {
                  estudianteService.create(estudiante).map(id => Right(s"Estudiante registrado con ID: $id"))
                }
              case Left(err) => IO(Left(err))
            }
          }

      // Estudiantes - PUT actualizar (con token)
      val updateEstudiante =
        estudiantesBaseEndpoint.put
          .in(jsonBody[Estudiante])
          .in(header[String]("X-Token"))
          .out(jsonBody[String])
          .serverLogic[IO] { case (estudiante, token) =>
            validateToken(token).flatMap {
              case Right(_) => estudianteService.update(estudiante).map {
                case 1 => Right("Estudiante actualizado correctamente")
                case _ => Right("Estudiante no encontrado")
              }
              case Left(err) => IO(Left(err))
            }
          }

      // Estudiantes - DELETE (con token)
      val deleteEstudiante =
        estudiantesBaseEndpoint.delete
          .in(path[Int]("id"))
          .in(header[String]("X-Token"))
          .out(jsonBody[String])
          .serverLogic[IO] { case (id, token) =>
            validateToken(token).flatMap {
              case Right(_) => estudianteService.delete(id).map {
                case 1 => Right("Estudiante eliminado correctamente")
                case _ => Right("Estudiante no encontrado")
              }
              case Left(err) => IO(Left(err))
            }
          }

      val swaggerEndpoints = SwaggerInterpreter()
          .fromServerEndpoints[IO](
            List(healthRoute, registerUsuarioRoute, loginRoute, getAllEstudiantes, getEstudianteById, createEstudiante, updateEstudiante, deleteEstudiante),
            "API Estudiantes",
            "1.0"
          )

      val swaggerRoutes = Http4sServerInterpreter[IO]().toRoutes(swaggerEndpoints)

      val apiRoutes = Http4sServerInterpreter[IO]().toRoutes(
        List(healthRoute, registerUsuarioRoute, loginRoute, getAllEstudiantes, getEstudianteById, createEstudiante, updateEstudiante, deleteEstudiante)
      )

      val allRoutes = swaggerRoutes <+> apiRoutes

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
