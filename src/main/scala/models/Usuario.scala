package models

case class Usuario(
  id: Int,
  username: String,
  password: String,
  apiToken: Option[String]
)

case class LoginRequest(
  username: String,
  password: String
)

case class LoginResponse(
  token: String,
  message: String
)
