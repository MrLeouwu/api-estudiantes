error id: file:///C:/Users/miros/api-estudiantes/src/main/scala/main.scala:List#
file:///C:/Users/miros/api-estudiantes/src/main/scala/main.scala
empty definition using pc, found symbol in pc: 
empty definition using semanticdb
empty definition using fallback
non-local guesses:
	 -cats/effect/List#
	 -org/http4s/ember/server/List#
	 -sttp/tapir/List#
	 -sttp/tapir/server/http4s/List#
	 -cats/implicits/List#
	 -models/List#
	 -repository/List#
	 -service/List#
	 -List#
	 -scala/Predef.List#
offset: 390
uri: file:///C:/Users/miros/api-estudiantes/src/main/scala/main.scala
text:
```scala
import cats.effect._
import org.http4s.ember.server._
import org.http4s.server.Router
import sttp.tapir._
import sttp.tapir.server.http4s._
import sttp.tapir.swagger.bundle.SwaggerInterpreter
import cats.implicits._

import database.Database
import models._
import repository._
import service._

object Main extends IOApp {

  val baseEndpoint = endpoint.in("estudiantes")

  def run(args: @@List[String]): IO[ExitCode] =
    Database.transactor.use { xa =>

      val repository = new EstudianteRepository(xa)
      val service = new EstudianteService(repository)

      // ENDPOINTS

      val getAll = baseEndpoint.get
        .out(jsonBody[List[Estudiante]])
        .serverLogic(_ => service.getAll.map(Right(_)))

      val getById = baseEndpoint.get
        .in(path[Int]("id"))
        .out(jsonBody[Option[Estudiante]])
        .serverLogic(id => service.getById(id).map(Right(_)))

      val create = baseEndpoint.post
        .in(jsonBody[Estudiante])
        .out(jsonBody[Int])
        .serverLogic(est => service.create(est).map(Right(_)))

      val delete = baseEndpoint.delete
        .in(path[Int]("id"))
        .out(jsonBody[Int])
        .serverLogic(id => service.delete(id).map(Right(_)))

      val routes =
        Http4sServerInterpreter[IO]()
          .toRoutes(List(getAll, getById, create, delete))

      val swaggerRoutes =
        Http4sServerInterpreter[IO]()
          .toRoutes(
            SwaggerInterpreter().fromServerEndpoints[IO](
              List(getAll, getById, create, delete),
              "API Estudiantes",
              "1.0"
            )
          )

      val finalHttpApp = (routes <+> swaggerRoutes).orNotFound

      EmberServerBuilder.default[IO]
        .withHost("0.0.0.0")
        .withPort(8080)
        .withHttpApp(finalHttpApp)
        .build
        .useForever
        .as(ExitCode.Success)
    }
}
```


#### Short summary: 

empty definition using pc, found symbol in pc: 