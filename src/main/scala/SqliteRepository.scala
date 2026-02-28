package repository

import models._
import java.sql.{Connection, DriverManager}
import java.io.File

class SqliteRepository(dbPath: String = "/app/estudiantes.db") {
  Class.forName("org.sqlite.JDBC")
  
  private val dbFile = new File(dbPath)
  dbFile.getParentFile.mkdirs()
  
  private val url = s"jdbc:sqlite:$dbPath"
  
  private def getConnection: Connection = DriverManager.getConnection(url)
  
  def init(): Unit = {
    val conn = getConnection
    try {
      conn.createStatement().execute(
        "CREATE TABLE IF NOT EXISTS usuarios (" +
        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
        "username TEXT UNIQUE NOT NULL, " +
        "password TEXT NOT NULL, " +
        "api_token TEXT)"
      )
      conn.createStatement().execute(
        "CREATE TABLE IF NOT EXISTS estudiantes (" +
        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
        "nombre TEXT NOT NULL, " +
        "edad INTEGER NOT NULL)"
      )
    } finally {
      conn.close()
    }
  }
  
  def createUsuario(username: String, password: String): Int = {
    val conn = getConnection
    try {
      val stmt = conn.prepareStatement("INSERT INTO usuarios (username, password) VALUES (?, ?)")
      stmt.setString(1, username)
      stmt.setString(2, password)
      stmt.executeUpdate()
      
      val rs = conn.createStatement().executeQuery("SELECT last_insert_rowid() as id")
      if (rs.next()) rs.getInt("id") else 0
    } finally {
      conn.close()
    }
  }
  
  def findUsuarioByUsername(username: String): Option[Usuario] = {
    val conn = getConnection
    try {
      val stmt = conn.prepareStatement("SELECT id, username, password, api_token FROM usuarios WHERE username = ?")
      stmt.setString(1, username)
      val rs = stmt.executeQuery()
      if (rs.next()) {
        val token = rs.getString("api_token")
        Some(Usuario(rs.getInt("id"), rs.getString("username"), rs.getString("password"), 
          if (token == null) "" else token))
      } else None
    } finally {
      conn.close()
    }
  }
  
  def findUsuarioByToken(token: String): Option[Usuario] = {
    val conn = getConnection
    try {
      val stmt = conn.prepareStatement("SELECT id, username, password, api_token FROM usuarios WHERE api_token = ?")
      stmt.setString(1, token)
      val rs = stmt.executeQuery()
      if (rs.next()) {
        val tokenStr = rs.getString("api_token")
        Some(Usuario(rs.getInt("id"), rs.getString("username"), rs.getString("password"),
          if (tokenStr == null) "" else tokenStr))
      } else None
    } finally {
      conn.close()
    }
  }
  
  def updateToken(id: Int, token: String): Unit = {
    val conn = getConnection
    try {
      val stmt = conn.prepareStatement("UPDATE usuarios SET api_token = ? WHERE id = ?")
      stmt.setString(1, token)
      stmt.setInt(2, id)
      stmt.executeUpdate()
    } finally {
      conn.close()
    }
  }
  
  def getAllEstudiantes: List[Estudiante] = {
    val conn = getConnection
    try {
      val rs = conn.createStatement().executeQuery("SELECT id, nombre, edad FROM estudiantes")
      var list = List[Estudiante]()
      while (rs.next()) {
        list = Estudiante(rs.getInt("id"), rs.getString("nombre"), rs.getInt("edad")) :: list
      }
      list.reverse
    } finally {
      conn.close()
    }
  }
  
  def getEstudianteById(id: Int): Option[Estudiante] = {
    val conn = getConnection
    try {
      val stmt = conn.prepareStatement("SELECT id, nombre, edad FROM estudiantes WHERE id = ?")
      stmt.setInt(1, id)
      val rs = stmt.executeQuery()
      if (rs.next()) {
        Some(Estudiante(rs.getInt("id"), rs.getString("nombre"), rs.getInt("edad")))
      } else None
    } finally {
      conn.close()
    }
  }
  
  def createEstudiante(nombre: String, edad: Int): Int = {
    val conn = getConnection
    try {
      val stmt = conn.prepareStatement("INSERT INTO estudiantes (nombre, edad) VALUES (?, ?)")
      stmt.setString(1, nombre)
      stmt.setInt(2, edad)
      stmt.executeUpdate()
      
      val rs = conn.createStatement().executeQuery("SELECT last_insert_rowid() as id")
      if (rs.next()) rs.getInt("id") else 0
    } finally {
      conn.close()
    }
  }
  
  def updateEstudiante(id: Int, nombre: String, edad: Int): Boolean = {
    val conn = getConnection
    try {
      val stmt = conn.prepareStatement("UPDATE estudiantes SET nombre = ?, edad = ? WHERE id = ?")
      stmt.setString(1, nombre)
      stmt.setInt(2, edad)
      stmt.setInt(3, id)
      stmt.executeUpdate() > 0
    } finally {
      conn.close()
    }
  }
  
  def deleteEstudiante(id: Int): Boolean = {
    val conn = getConnection
    try {
      val stmt = conn.prepareStatement("DELETE FROM estudiantes WHERE id = ?")
      stmt.setInt(1, id)
      stmt.executeUpdate() > 0
    } finally {
      conn.close()
    }
  }
}
