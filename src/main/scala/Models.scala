package models

case class Usuario(
  id: Int = 0,
  username: String = "",
  password: String = "",
  apiToken: String = ""
)

case class Estudiante(
  id: Int = 0,
  nombre: String = "",
  edad: Int = 0
)

case class LoginRequest(
  username: String,
  password: String
)

case class LoginResponse(
  token: String,
  message: String
)

case class HealthResponse(
  status: String,
  timestamp: String
)
