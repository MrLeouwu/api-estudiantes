package main

import cats.effect.{IO, IOApp, ExitCode}
import cats.implicits._
import org.http4s._
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.dsl.io._
import org.http4s.implicits._
import org.http4s.circe._
import io.circe.Json
import io.circe.parser
import org.http4s.Header
import com.comcast.ip4s._
import java.time.LocalDateTime
import models._
import repository.SqliteRepository
import java.util.UUID
import scala.util.Try

object Main extends IOApp {
  
  val repo = new SqliteRepository()
  repo.init()
  
  val swaggerHtml = """<!DOCTYPE html>
<html>
<head>
  <title>API Estudiantes - Documentación</title>
  <link rel="stylesheet" type="text/css" href="https://cdn.jsdelivr.net/npm/redoc@next/bundles/redoc.standalone.js">
</head>
<body>
  <redoc spec-url='/api/v1/openapi.json'></redoc>
  <script src="https://cdn.jsdelivr.net/npm/redoc@next/bundles/redoc.standalone.js"></script>
</body>
</html>"""
  
  val openapiJson = """{
    "openapi": "3.0.0",
    "info": {
      "title": "API Estudiantes",
      "version": "1.0.0",
      "description": "API para gestionar estudiantes con autenticación"
    },
    "servers": [{ "url": "/api/v1" }],
    "paths": {
      "/health": {
        "get": {
          "summary": "Health check",
          "responses": { "200": { "description": "OK" } }
        }
      },
      "/usuarios": {
        "post": {
          "summary": "Registrar usuario",
          "requestBody": {
            "content": {
              "application/json": {
                "schema": {
                  "type": "object",
                  "properties": {
                    "username": { "type": "string" },
                    "password": { "type": "string" }
                  }
                }
              }
            }
          },
          "responses": { "200": { "description": "Usuario registrado" } }
        }
      },
      "/login": {
        "post": {
          "summary": "Iniciar sesión",
          "requestBody": {
            "content": {
              "application/json": {
                "schema": {
                  "type": "object",
                  "properties": {
                    "username": { "type": "string" },
                    "password": { "type": "string" }
                  }
                }
              }
            }
          },
          "responses": { "200": { "description": "Login exitoso", "content": { "application/json": { "schema": { "type": "object", "properties": { "token": { "type": "string" } } } } } }
        }
      },
      "/estudiantes": {
        "get": {
          "summary": "Listar estudiantes",
          "security": [{ "bearerAuth": [] }],
          "responses": { "200": { "description": "Lista de estudiantes" } }
        },
        "post": {
          "summary": "Crear estudiante",
          "security": [{ "bearerAuth": [] }],
          "requestBody": {
            "content": {
              "application/json": {
                "schema": {
                  "type": "object",
                  "properties": {
                    "nombre": { "type": "string" },
                    "edad": { "type": "integer" }
                  }
                }
              }
            }
          },
          "responses": { "200": { "description": "Estudiante creado" } }
        }
      },
      "/estudiantes/{id}": {
        "get": {
          "summary": "Obtener estudiante por ID",
          "security": [{ "bearerAuth": [] }],
          "parameters": [{ "name": "id", "in": "path", "required": true, "schema": { "type": "integer" } }],
          "responses": { "200": { "description": "Estudiante encontrado" } }
        },
        "put": {
          "summary": "Actualizar estudiante",
          "security": [{ "bearerAuth": [] }],
          "parameters": [{ "name": "id", "in": "path", "required": true, "schema": { "type": "integer" } }],
          "requestBody": {
            "content": {
              "application/json": {
                "schema": {
                  "type": "object",
                  "properties": {
                    "id": { "type": "integer" },
                    "nombre": { "type": "string" },
                    "edad": { "type": "integer" }
                  }
                }
              }
            }
          },
          "responses": { "200": { "description": "Estudiante actualizado" } }
        },
        "delete": {
          "summary": "Eliminar estudiante",
          "security": [{ "bearerAuth": [] }],
          "parameters": [{ "name": "id", "in": "path", "required": true, "schema": { "type": "integer" } }],
          "responses": { "200": { "description": "Estudiante eliminado" } }
        }
      }
    },
    "components": {
      "securitySchemes": {
        "bearerAuth": {
          "type": "http",
          "scheme": "bearer",
          " bearerFormat": "UUID"
        }
      }
    }
  }"""
  
  def getToken(headers: Headers): Option[String] = {
    headers.headers.find(_.name.value == "X-Token").map(_.value)
  }
  
  val routes = HttpRoutes.of[IO] {
    
    case GET -> Root / "docs" =>
      Response[IO](status = Status.Ok)
        .withEntity(swaggerHtml)
        .putHeaders(Header("Content-Type", "text/html"))
        .pure[IO]
    
    case GET -> Root / "docs" / "" =>
      Response[IO](status = Status.Ok)
        .withEntity(swaggerHtml)
        .putHeaders(Header("Content-Type", "text/html"))
        .pure[IO]
    
    case GET -> Root / "api" / "v1" / "openapi.json" =>
      Response[IO](status = Status.Ok)
        .withEntity(openapiJson)
        .putHeaders(Header("Content-Type", "application/json"))
        .pure[IO]
    
    case GET -> Root / "api" / "v1" / "health" =>
      Ok(Json.obj("status" -> Json.fromString("OK"), "timestamp" -> Json.fromString(LocalDateTime.now.toString)))
    
    case req @ POST -> Root / "api" / "v1" / "usuarios" =>
      req.as[String].flatMap { body =>
        Try(parser.parse(body)).toOption match {
          case Some(Right(json)) =>
            val username = json.hcursor.downField("username").as[String].getOrElse("")
            val password = json.hcursor.downField("password").as[String].getOrElse("")
            if (username.isEmpty || password.isEmpty) {
              BadRequest(Json.obj("error" -> Json.fromString("Usuario y password requeridos")))
            } else {
              try {
                val id = repo.createUsuario(username, password)
                Ok(Json.obj("message" -> Json.fromString(s"Usuario registrado. Ahora puedes hacer login.")))
              } catch {
                case e: Exception => 
                  InternalServerError(Json.obj("error" -> Json.fromString(e.getMessage)))
              }
            }
          case _ =>
            BadRequest(Json.obj("error" -> Json.fromString("JSON inválido")))
        }
      }
    
    case req @ POST -> Root / "api" / "v1" / "login" =>
      req.as[String].flatMap { body =>
        Try(parser.parse(body)).toOption match {
          case Some(Right(json)) =>
            val username = json.hcursor.downField("username").as[String].getOrElse("")
            val password = json.hcursor.downField("password").as[String].getOrElse("")
            repo.findUsuarioByUsername(username) match {
              case Some(u) if u.password == password =>
                val token = UUID.randomUUID().toString
                repo.updateToken(u.id, token)
                Ok(Json.obj("token" -> Json.fromString(token), "message" -> Json.fromString("Login exitoso")))
              case _ =>
                BadRequest(Json.obj("error" -> Json.fromString("Usuario o contraseña incorrectos")))
            }
          case _ =>
            BadRequest(Json.obj("error" -> Json.fromString("JSON inválido")))
        }
      }
    
    case req @ GET -> Root / "api" / "v1" / "estudiantes" =>
      getToken(req.headers) match {
        case Some(token) if repo.findUsuarioByToken(token).isDefined =>
          Ok(Json.fromValues(repo.getAllEstudiantes.map(e => 
            Json.obj("id" -> Json.fromInt(e.id), "nombre" -> Json.fromString(e.nombre), "edad" -> Json.fromInt(e.edad))
          )))
        case _ =>
          Forbidden(Json.obj("error" -> Json.fromString("Token requerido")))
      }
    
    case req @ POST -> Root / "api" / "v1" / "estudiantes" =>
      getToken(req.headers) match {
        case Some(token) if repo.findUsuarioByToken(token).isDefined =>
          req.as[String].flatMap { body =>
            Try(parser.parse(body)).toOption match {
              case Some(Right(json)) =>
                val nombre = json.hcursor.downField("nombre").as[String].getOrElse("")
                val edad = json.hcursor.downField("edad").as[Int].getOrElse(0)
                if (nombre.isEmpty) {
                  BadRequest(Json.obj("error" -> Json.fromString("Nombre requerido")))
                } else {
                  val id = repo.createEstudiante(nombre, edad)
                  Ok(Json.obj("message" -> Json.fromString(s"Estudiante registrado con ID: $id")))
                }
              case _ =>
                BadRequest(Json.obj("error" -> Json.fromString("JSON inválido")))
            }
          }
        case _ =>
          Forbidden(Json.obj("error" -> Json.fromString("Token requerido")))
      }
    
    case req @ GET -> Root / "api" / "v1" / "estudiantes" / IntVar(id) =>
      getToken(req.headers) match {
        case Some(token) if repo.findUsuarioByToken(token).isDefined =>
          repo.getEstudianteById(id) match {
            case Some(e) => Ok(Json.obj("id" -> Json.fromInt(e.id), "nombre" -> Json.fromString(e.nombre), "edad" -> Json.fromInt(e.edad)))
            case None => NotFound(Json.obj("error" -> Json.fromString("No encontrado")))
          }
        case _ =>
          Forbidden(Json.obj("error" -> Json.fromString("Token requerido")))
      }
    
    case req @ PUT -> Root / "api" / "v1" / "estudiantes" =>
      getToken(req.headers) match {
        case Some(token) if repo.findUsuarioByToken(token).isDefined =>
          req.as[String].flatMap { body =>
            Try(parser.parse(body)).toOption match {
              case Some(Right(json)) =>
                val id = json.hcursor.downField("id").as[Int].getOrElse(0)
                val nombre = json.hcursor.downField("nombre").as[String].getOrElse("")
                val edad = json.hcursor.downField("edad").as[Int].getOrElse(0)
                if (id == 0 || nombre.isEmpty) {
                  BadRequest(Json.obj("error" -> Json.fromString("ID y nombre requeridos")))
                } else {
                  val ok = repo.updateEstudiante(id, nombre, edad)
                  if (ok) Ok(Json.obj("message" -> Json.fromString("Estudiante actualizado")))
                  else NotFound(Json.obj("error" -> Json.fromString("No encontrado")))
                }
              case _ =>
                BadRequest(Json.obj("error" -> Json.fromString("JSON inválido")))
            }
          }
        case _ =>
          Forbidden(Json.obj("error" -> Json.fromString("Token requerido")))
      }
    
    case req @ DELETE -> Root / "api" / "v1" / "estudiantes" / IntVar(id) =>
      getToken(req.headers) match {
        case Some(token) if repo.findUsuarioByToken(token).isDefined =>
          val ok = repo.deleteEstudiante(id)
          if (ok) Ok(Json.obj("message" -> Json.fromString("Estudiante eliminado")))
          else NotFound(Json.obj("error" -> Json.fromString("No encontrado")))
        case _ =>
          Forbidden(Json.obj("error" -> Json.fromString("Token requerido")))
      }
  }
  
  override def run(args: List[String]): IO[ExitCode] = {
    EmberServerBuilder.default[IO]
      .withHost(host"0.0.0.0")
      .withPort(port"8080")
      .withHttpApp(routes.orNotFound)
      .build
      .useForever
      .as(ExitCode.Success)
  }
}
