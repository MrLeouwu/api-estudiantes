error id: file:///C:/Users/miros/api-estudiantes/src/main/scala/main.scala:getConnection.
file:///C:/Users/miros/api-estudiantes/src/main/scala/main.scala
empty definition using pc, found symbol in pc: 
empty definition using semanticdb
empty definition using fallback
non-local guesses:
	 -cats/effect/DriverManager.getConnection.
	 -cats/effect/DriverManager.getConnection#
	 -cats/effect/DriverManager.getConnection().
	 -org/http4s/DriverManager.getConnection.
	 -org/http4s/DriverManager.getConnection#
	 -org/http4s/DriverManager.getConnection().
	 -org/http4s/dsl/io/DriverManager.getConnection.
	 -org/http4s/dsl/io/DriverManager.getConnection#
	 -org/http4s/dsl/io/DriverManager.getConnection().
	 -org/http4s/implicits/DriverManager.getConnection.
	 -org/http4s/implicits/DriverManager.getConnection#
	 -org/http4s/implicits/DriverManager.getConnection().
	 -org/http4s/circe/DriverManager.getConnection.
	 -org/http4s/circe/DriverManager.getConnection#
	 -org/http4s/circe/DriverManager.getConnection().
	 -org/http4s/circe/CirceEntityCodec.DriverManager.getConnection.
	 -org/http4s/circe/CirceEntityCodec.DriverManager.getConnection#
	 -org/http4s/circe/CirceEntityCodec.DriverManager.getConnection().
	 -io/circe/generic/auto/DriverManager.getConnection.
	 -io/circe/generic/auto/DriverManager.getConnection#
	 -io/circe/generic/auto/DriverManager.getConnection().
	 -io/circe/syntax/DriverManager.getConnection.
	 -io/circe/syntax/DriverManager.getConnection#
	 -io/circe/syntax/DriverManager.getConnection().
	 -java/sql/DriverManager.getConnection.
	 -java/sql/DriverManager.getConnection#
	 -java/sql/DriverManager.getConnection().
	 -DriverManager.getConnection.
	 -DriverManager.getConnection#
	 -DriverManager.getConnection().
	 -scala/Predef.DriverManager.getConnection.
	 -scala/Predef.DriverManager.getConnection#
	 -scala/Predef.DriverManager.getConnection().
offset: 468
uri: file:///C:/Users/miros/api-estudiantes/src/main/scala/main.scala
text:
```scala
import cats.effect._
import org.http4s._
import org.http4s.dsl.io._
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.implicits._
import org.http4s.circe._
import org.http4s.circe.CirceEntityCodec._
import io.circe.generic.auto._
import io.circe.syntax._
import java.sql.DriverManager

case class Estudiante(id: Int, nombre: String, edad: Int, carrera: String)

object Main extends IOApp {

  def createTable(): Unit = {
    val conn = DriverManager.@@getConnection("jdbc:sqlite:estudiantes.db")
    val stmt = conn.createStatement()
    stmt.execute(
      """
      CREATE TABLE IF NOT EXISTS estudiantes (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        nombre TEXT,
        edad INTEGER,
        carrera TEXT
      )
      """
    )
    conn.close()
  }

  val routes = HttpRoutes.of[IO] {

    // GET todos
    case GET -> Root / "estudiantes" =>
      val conn = DriverManager.getConnection("jdbc:sqlite:estudiantes.db")
      val stmt = conn.createStatement()
      val rs = stmt.executeQuery("SELECT * FROM estudiantes")

      var lista = List[Estudiante]()

      while (rs.next()) {
        lista = lista :+ Estudiante(
          rs.getInt("id"),
          rs.getString("nombre"),
          rs.getInt("edad"),
          rs.getString("carrera")
        )
      }

      conn.close()
      Ok(lista.asJson)

    // POST crear
    case req @ POST -> Root / "estudiantes" =>
      for {
        estudiante <- req.as[Estudiante]
        _ <- IO {
          val conn = DriverManager.getConnection("jdbc:sqlite:estudiantes.db")
          val stmt = conn.prepareStatement(
            "INSERT INTO estudiantes (nombre, edad, carrera) VALUES (?, ?, ?)"
          )
          stmt.setString(1, estudiante.nombre)
          stmt.setInt(2, estudiante.edad)
          stmt.setString(3, estudiante.carrera)
          stmt.executeUpdate()
          conn.close()
        }
        response <- Ok("Estudiante creado con éxito")
      } yield response
  }.orNotFound

  def run(args: List[String]): IO[ExitCode] = {
    IO(createTable()) *>
      EmberServerBuilder
        .default[IO]
        .withHttpApp(routes)
        .build
        .useForever
        .as(ExitCode.Success)
  }
}
```


#### Short summary: 

empty definition using pc, found symbol in pc: 