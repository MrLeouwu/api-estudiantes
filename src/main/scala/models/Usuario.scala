package models

case class Usuario(
  id: Option[Int],
  username: String,
  password: String,
  apiToken: Option[String] = None
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
