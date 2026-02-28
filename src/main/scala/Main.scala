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
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>API Estudiantes - Documentación</title>
  <link rel="stylesheet" type="text/css" href="https://cdn.jsdelivr.net/npm/swagger-ui-dist@5/swagger-ui.css">
  <style>
    body { margin: 0; padding: 0; }
  </style>
</head>
<body>
  <div id="swagger-ui"></div>
  <script src="https://cdn.jsdelivr.net/npm/swagger-ui-dist@5/swagger-ui-bundle.js"></script>
  <script>
    window.onload = function() {
      SwaggerUIBundle({
        url: '/api/v1/openapi.json',
        dom_id: '#swagger-ui',
        deepLinking: true,
        presets: [
          SwaggerUIBundle.presets.apis,
          'SwaggerUIBundle.something'
        ]
      });
    };
  </script>
</body>
</html>"""
  
  val openapiJson = """{
    "openapi": "3.0.0",
    "info": {
      "title": "API Estudiantes",
      "version": "1.0.0",
      "description": "API para gestionar estudiantes con autenticacion"
    },
    "servers": [{"url": "/api/v1"}],
    "paths": {
      "/health": {
        "get": {
          "summary": "Health check",
          "responses": {"200": {"description": "OK"}}
        }
      },
      "/usuarios": {
        "post": {
          "summary": "Registrar usuario",
          "description": "Registrar un nuevo usuario en el sistema",
          "requestBody": {
            "required": true,
            "content": {
              "application/json": {
                "schema": {
                  "type": "object",
                  "required": ["username", "password"],
                  "properties": {
                    "username": {"type": "string", "example": "admin"},
                    "password": {"type": "string", "example": "password123"}
                  }
                }
              }
            }
          },
          "responses": {"200": {"description": "Usuario registrado"}}
        }
      },
      "/login": {
        "post": {
          "summary": "Iniciar sesion",
          "description": "Iniciar sesion y obtener token de acceso",
          "requestBody": {
            "required": true,
            "content": {
              "application/json": {
                "schema": {
                  "type": "object",
                  "required": ["username", "password"],
                  "properties": {
                    "username": {"type": "string", "example": "admin"},
                    "password": {"type": "string", "example": "password123"}
                  }
                }
              }
            }
          },
          "responses": {
            "200": {
              "description": "Login exitoso",
              "content": {
                "application/json": {
                  "example": {"token": "uuid-aqui", "message": "Login exitoso"}
                }
              }
            }
          }
        }
      },
      "/estudiantes": {
        "get": {
          "summary": "Listar estudiantes",
          "description": "Obtener todos los estudiantes",
          "security": [{"bearerAuth": []}],
          "parameters": [
            {
              "name": "X-Token",
              "in": "header",
              "required": true,
              "schema": {"type": "string"},
              "example": "tu-token-aqui"
            }
          ],
          "responses": {
            "200": {
              "description": "Lista de estudiantes",
              "content": {
                "application/json": {
                  "example": [{"id": 1, "nombre": "Juan", "edad": 20}]
                }
              }
            }
          }
        },
        "post": {
          "summary": "Crear estudiante",
          "description": "Crear un nuevo estudiante",
          "security": [{"bearerAuth": []}],
          "parameters": [
            {
              "name": "X-Token",
              "in": "header",
              "required": true,
              "schema": {"type": "string"},
              "example": "tu-token-aqui"
            }
          ],
          "requestBody": {
            "required": true,
            "content": {
              "application/json": {
                "schema": {
                  "type": "object",
                  "required": ["nombre"],
                  "properties": {
                    "nombre": {"type": "string", "example": "Juan Perez"},
                    "edad": {"type": "integer", "example": 20}
                  }
                }
              }
            }
          },
          "responses": {"200": {"description": "Estudiante creado"}}
        }
      },
      "/estudiantes/{id}": {
        "get": {
          "summary": "Obtener estudiante por ID",
          "description": "Obtener un estudiante especifico",
          "security": [{"bearerAuth": []}],
          "parameters": [
            {"name": "id", "in": "path", "required": true, "schema": {"type": "integer"}, "example": 1},
            {"name": "X-Token", "in": "header", "required": true, "schema": {"type": "string"}, "example": "tu-token-aqui"}
          ],
          "responses": {"200": {"description": "Estudiante encontrado"}}
        },
        "put": {
          "summary": "Actualizar estudiante",
          "description": "Actualizar un estudiante existente",
          "security": [{"bearerAuth": []}],
          "parameters": [
            {"name": "id", "in": "path", "required": true, "schema": {"type": "integer"}, "example": 1},
            {"name": "X-Token", "in": "header", "required": true, "schema": {"type": "string"}, "example": "tu-token-aqui"}
          ],
          "requestBody": {
            "required": true,
            "content": {
              "application/json": {
                "schema": {
                  "type": "object",
                  "properties": {
                    "id": {"type": "integer", "example": 1},
                    "nombre": {"type": "string", "example": "Juan Actualizado"},
                    "edad": {"type": "integer", "example": 21}
                  }
                }
              }
            }
          },
          "responses": {"200": {"description": "Estudiante actualizado"}}
        },
        "delete": {
          "summary": "Eliminar estudiante",
          "description": "Eliminar un estudiante",
          "security": [{"bearerAuth": []}],
          "parameters": [
            {"name": "id", "in": "path", "required": true, "schema": {"type": "integer"}, "example": 1},
            {"name": "X-Token", "in": "header", "required": true, "schema": {"type": "string"}, "example": "tu-token-aqui"}
          ],
          "responses": {"200": {"description": "Estudiante eliminado"}}
        }
      }
    },
    "components": {
      "securitySchemes": {
        "bearerAuth": {
          "type": "apiKey",
          "name": "X-Token",
          "in": "header",
          "description": "Token de acceso"
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
