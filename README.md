# API Estudiantes

API REST para gestión de estudiantes con autenticación JWT, desarrollada en Scala.

## Tabla de Contenidos

- [Descripción](#descripción)
- [Tecnologías](#tecnologías)
- [Requisitos](#requisitos)
- [Instalación](#instalación)
- [Configuración](#configuración)
- [Ejecución](#ejecución)
- [Autenticación](#autenticación)
- [API Endpoints](#api-endpoints)
- [Ejemplos de Uso](#ejemplos-de-uso)
- [Documentación Swagger](#documentación-swagger)
- [Estructura del Proyecto](#estructura-del-proyecto)
- [Variables de Entorno](#variables-de-entorno)
- [Desarrollo](#desarrollo)
- [Licencia](#licencia)

## Descripción

API RESTful para la gestión de estudiantes con las siguientes funcionalidades:

- Registro y login de usuarios
- Autenticación mediante tokens JWT
- CRUD completo de estudiantes
- Documentación interactiva con Swagger UI
- Base de datos MySQL persistente
- Contenedorizado con Docker

## Tecnologías

| Tecnología | Versión | Descripción |
|------------|---------|-------------|
| Scala | 2.13 | Lenguaje de programación |
| http4s | 0.23.26 | Framework web HTTP |
| Doobie | 1.0.0-RC5 | Biblioteca de acceso a base de datos |
| MySQL | 8 | Sistema de gestión de base de datos |
| Tapir | 1.9.10 | Definición de APIs + Swagger |
| JWT | 4.4.0 | Autenticación con tokens |
| Docker | Latest | Contenedorización |

## Requisitos

- **Docker** y **Docker Compose**
- **JDK 11** o superior
- **MySQL** (opcional, ya incluido en Docker)

## Instalación

1. **Clona el repositorio**

```bash
git clone <url-del-repositorio>
cd api-estudiantes
```

2. **Configura el entorno** (opcional)

Crea un archivo `.env` o modifica las variables en `docker-compose.yml` según tus necesidades.

## Configuración

### Base de Datos

La base de datos se crea automáticamente con el nombre `estudiantes_db`. Las tablas se crean manualmente o desde el script `init.sql`.

```sql
-- Tabla de estudiantes
CREATE TABLE estudiantes (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(255) NOT NULL,
    edad INT NOT NULL
);

-- Tabla de usuarios
CREATE TABLE usuarios (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    api_token VARCHAR(255) UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### Variables de Entorno

| Variable | Default | Descripción |
|----------|---------|-------------|
| `DB_URL` | jdbc:mysql://mysql:3306/estudiantes_db?serverTimezone=UTC | URL de conexión a MySQL |
| `DB_USER` | root | Usuario de MySQL |
| `DB_PASS` | (vacío) | Contraseña de MySQL |
| `JWT_SECRET` | api-estudiantes-secret-key-production-2024 | Clave secreta para JWT |

## Ejecución

### Con Docker (Recomendado)

```bash
# Iniciar servicios
docker-compose up -d

# Ver logs
docker-compose logs -f

# Detener servicios
docker-compose down
```

### Servicios

- **API**: http://localhost:8080
- **MySQL**: localhost:3306
- **Swagger UI**: http://localhost:8080/docs

### Nota Importante

Si tienes MySQL local instalado, detén el servicio antes de iniciar Docker:

```bash
# En Windows (XAMPP/WAMP)
Detén MySQL desde el panel de control

# En Linux
sudo systemctl stop mysql
```

## Autenticación

La API usa **tokens JWT** para autenticar usuarios. Todos los endpoints protegidos requieren el token como parámetro de query.

### Flujo de Autenticación

1. El usuario se registra con `POST /auth/register`
2. El usuario hace login con `POST /auth/login` y obtiene un token
3. El token se envía en todas las peticiones como `?token=...`

## API Endpoints

### Autenticación

| Método | Endpoint | Descripción | Auth |
|--------|----------|-------------|------|
| POST | `/auth/register` | Registrar nuevo usuario | No |
| POST | `/auth/login` | Iniciar sesión y obtener token | No |

### Estudiantes

| Método | Endpoint | Descripción | Auth |
|--------|----------|-------------|------|
| GET | `/api/estudiantes?token=` | Listar todos los estudiantes | Sí |
| GET | `/api/estudiantes/{id}?token=` | Obtener estudiante por ID | Sí |
| POST | `/api/estudiantes?token=` | Crear nuevo estudiante | Sí |
| DELETE | `/api/estudiantes/{id}?token=` | Eliminar estudiante | Sí |

### Documentación

| Método | Endpoint | Descripción | Auth |
|--------|----------|-------------|------|
| GET | `/docs` | Documentación Swagger UI | Sí |

## Ejemplos de Uso

### Usando cURL

```bash
# 1. Registrar usuario
curl -X POST http://localhost:8080/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"password123"}'

# 2. Login (obtener token)
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"password123"}'

# Guardar token en variable
TOKEN="tu_token_aqui"

# 3. Listar estudiantes
curl "http://localhost:8080/api/estudiantes?token=$TOKEN"

# 4. Crear estudiante
curl -X POST "http://localhost:8080/api/estudiantes?token=$TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"nombre":"Juan","edad":20}'

# 5. Obtener estudiante por ID
curl "http://localhost:8080/api/estudiantes/1?token=$TOKEN"

# 6. Eliminar estudiante
curl -X DELETE "http://localhost:8080/api/estudiantes/1?token=$TOKEN"
```

### Usando Swagger UI

1. Abre http://localhost:8080/docs en tu navegador
2. Expande el endpoint `POST /auth/register` y ejecuta para registrarte
3. Expande `POST /auth/login`, ejecuta y copia el `token` del response
4. En cada endpoint, pega el token en el parámetro `token`
5. Click en "Execute" para probar

### Respuestas de Ejemplo

**Login exitoso:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "message": "Login exitoso"
}
```

**Listar estudiantes:**
```json
[
  {
    "id": 1,
    "nombre": "Juan",
    "edad": 20
  }
]
```

**Crear estudiante:**
```json
1
```

## Estructura del Proyecto

```
api-estudiantes/
├── src/
│   └── main/
│       └── scala/
│           ├── main/
│           │   └── main.scala           # Punto de entrada
│           ├── models/
│           │   ├── Estudiante.scala     # Modelo de estudiante
│           │   └── Usuario.scala        # Modelo de usuario
│           ├── repository/
│           │   ├── EstudianteRepository.scala
│           │   └── UsuarioRepository.scala
│           └── service/
│               ├── EstudianteService.scala
│               └── AuthService.scala     # Servicio de autenticación
├── project/
│   ├── Dependencies.scala
│   └── plugins.sbt
├── build.sbt                    # Configuración de SBT
├── Dockerfile                   # Imagen de Docker
├── docker-compose.yml           # Orquestación de contenedores
└── init.sql                     # Script de base de datos
```

## Desarrollo

### Compilar Proyecto

```bash
cd api-estudiantes
sbt compile
```

### Crear JAR

```bash
cd api-estudiantes
sbt assembly
```

### Ejecutar Tests

```bash
cd api-estudiantes
sbt test
```

### Construir Imagen Docker

```bash
docker build -t api-estudiantes .
```

## Acceso a Base de Datos

### phpMyAdmin

- **Servidor**: localhost
- **Puerto**: 3306
- **Usuario**: root
- **Contraseña**: (vacía)
- **Base de datos**: estudiantes_db

### MySQL desde Línea de Comandos

```bash
docker exec -it mysql_estudiantes mysql -u root -p
```

## Solución de Problemas

### Error: Puerto 3306 en uso

Si tienes MySQL local instalado, detén el servicio antes de iniciar Docker:

```bash
# Windows
net stop mysql

# Linux
sudo systemctl stop mysql
```

### Error: Tablas no existen

Las tablas se crean manualmente o puedes recrear los contenedores:

```bash
docker-compose down -v
docker-compose up -d
```

### Ver Logs

```bash
# Todos los servicios
docker-compose logs -f

# Solo API
docker logs api_estudiantes

# Solo MySQL
docker logs mysql_estudiantes
```

## Licencia

Este proyecto está bajo la licencia MIT.
