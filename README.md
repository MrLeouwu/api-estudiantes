# API Estudiantes

API REST para gestión de estudiantes con autenticación, desarrollada en Scala con http4s y SQLite.

## Tabla de Contenidos

- [Descripción](#descripción)
- [Tecnologías](#tecnologías)
- [Endpoints](#endpoints)
- [Ejemplos de Uso](#ejemplos-de-uso)
- [Documentación](#documentación)

## Descripción

API RESTful para la gestión de estudiantes con las siguientes funcionalidades:

- Registro y login de usuarios
- Autenticación mediante tokens (header X-Token)
- CRUD completo de estudiantes
- Documentación interactiva con ReDoc
- Base de datos SQLite (persistente)
- Contenedorizado con Docker

## Tecnologías

| Tecnología | Versión | Descripción |
|------------|---------|-------------|
| Scala | 2.13 | Lenguaje de programación |
| http4s | 0.23.26 | Framework web HTTP |
| SQLite | 3.44.1 | Base de datos embebida |
| Docker | Latest | Contenedorización |

## Endpoints

### Documentación

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| GET | `/docs` | Documentación interactiva (ReDoc) |
| GET | `/api/v1/openapi.json` | Especificación OpenAPI JSON |

### Autenticación

| Método | Endpoint | Descripción | Auth |
|--------|----------|-------------|------|
| POST | `/api/v1/usuarios` | Registrar nuevo usuario | No |
| POST | `/api/v1/login` | Iniciar sesión y obtener token | No |

### Estudiantes (requieren header X-Token)

| Método | Endpoint | Descripción | Auth |
|--------|----------|-------------|------|
| GET | `/api/v1/estudiantes` | Listar todos los estudiantes | Sí (Token) |
| GET | `/api/v1/estudiantes/{id}` | Obtener estudiante por ID | Sí (Token) |
| POST | `/api/v1/estudiantes` | Crear nuevo estudiante | Sí (Token) |
| PUT | `/api/v1/estudiantes` | Actualizar estudiante | Sí (Token) |
| DELETE | `/api/v1/estudiantes/{id}` | Eliminar estudiante | Sí (Token) |

### Sistema

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| GET | `/api/v1/health` | Health check |

## Ejemplos de Uso

### Registro de usuario

```bash
curl -X POST https://tu-servicio.onrender.com/api/v1/usuarios \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"password123"}'
```

### Login (obtener token)

```bash
curl -X POST https://tu-servicio.onrender.com/api/v1/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"password123"}'
```

Respuesta:
```json
{
  "token": "uuid-aqui",
  "message": "Login exitoso"
}
```

### Listar estudiantes

```bash
curl https://tu-servicio.onrender.com/api/v1/estudiantes \
  -H "X-Token: tu-token-aqui"
```

### Crear estudiante

```bash
curl -X POST https://tu-servicio.onrender.com/api/v1/estudiantes \
  -H "X-Token: tu-token-aqui" \
  -H "Content-Type: application/json" \
  -d '{"nombre":"Juan Perez","edad":20}'
```

### Obtener estudiante por ID

```bash
curl https://tu-servicio.onrender.com/api/v1/estudiantes/1 \
  -H "X-Token: tu-token-aqui"
```

### Actualizar estudiante

```bash
curl -X PUT https://tu-servicio.onrender.com/api/v1/estudiantes \
  -H "X-Token: tu-token-aqui" \
  -H "Content-Type: application/json" \
  -d '{"id":1,"nombre":"Juan Actualizado","edad":21}'
```

### Eliminar estudiante

```bash
curl -X DELETE https://tu-servicio.onrender.com/api/v1/estudiantes/1 \
  -H "X-Token: tu-token-aqui"
```

## Documentación

La documentación interactiva está disponible en:

- **Producción**: https://tu-servicio.onrender.com/docs
- **Local**: http://localhost:8080/docs

La especificación OpenAPI está disponible en:

- **Producción**: https://tu-servicio.onrender.com/api/v1/openapi.json
- **Local**: http://localhost:8080/api/v1/openapi.json
