# Educa360 — Backend

API REST para la gestión académica de un colegio de **educación secundaria** (grados 1 a 5): usuarios y roles, catálogo académico (períodos, materias, secciones, cursos y horarios) y el núcleo de familias (estudiantes, matrículas y apoderados).

Construida con **Spring Boot** y **PostgreSQL**, documentada con **Swagger** y cubierta por una suite de pruebas de integración que levanta una base de datos efímera con **Testcontainers**.

---

## Stack

| Capa | Tecnología |
|---|---|
| Lenguaje | Java 25 |
| Framework | Spring Boot 4.1.1 (Spring Security, Spring Data JPA) |
| Base de datos | PostgreSQL 17 |
| Autenticación | JWT (bearer token) |
| Documentación | OpenAPI / Swagger UI |
| Empaquetado | Maven Wrapper (`./mvnw`) |
| Despliegue | Docker Compose (backend + Postgres) |
| Pruebas | JUnit 5 + MockMvc + Testcontainers |
| CI | GitHub Actions (en cada push) |

---

## Puesta en marcha

**Requisitos:** Docker y Docker Compose.

```bash
# 1) Configurar el entorno (el .env queda fuera de Git)
cp .env.example .env
#    - rellena POSTGRES_PASSWORD
#    - genera el secreto JWT:  openssl rand -base64 48

# 2) Levantar todo
docker compose up -d
```

| Servicio | URL |
|---|---|
| API | `http://localhost:8080` |
| Swagger UI | `http://localhost:8080/swagger-ui/index.html` |
| PostgreSQL | `127.0.0.1:5432` (sólo accesible desde la máquina) |

### Primer usuario

La base de datos arranca vacía y **el primer registro crea la cuenta `ADMIN`**:

```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@educa360.pe","password":"Admin12345","nombres":"Ana","apellidos":"Ruiz","dni":"10000001"}'
```

A partir de ahí, `/auth/register` queda deshabilitado: **todas las altas las hace un `ADMIN`** desde `/api/v1/admin/usuarios` (y las de alumnos/familias desde `/api/v1/secretaria/...`).

---

## Desarrollo local

```bash
# Sólo la base de datos en Docker y la app en local:
docker compose up -d postgres
./mvnw spring-boot:run

# Pruebas (requiere Docker: cada clase levanta su propia PostgreSQL efímera)
./mvnw test
```

---

## Roles y zonas de la API

Cinco roles; la **ruta nombra el dominio responsable** y el `@PreAuthorize` quién puede entrar. Sólo el primero de la tabla es público:

| Zona | Quién | Qué gestiona |
|---|---|---|
| `/api/v1/auth` | público / autenticado | `login`, `register` (sólo arranque) y `me` (cuenta + perfil del rol) |
| `/api/v1/admin/...` | `ADMIN` | Usuarios, docentes (+ habilitaciones), secretarias, materias, períodos, secciones, cursos (+ horario) |
| `/api/v1/secretaria/...` | `ADMIN` + `SECRETARIA` | Estudiantes, apoderados (+ representados) y matrículas |
| `/api/v1/docente/cursos` | `DOCENTE` | Sus cursos |
| `/api/v1/estudiante/cursos` | `ESTUDIANTE` | Los cursos de su sección |
| `/api/v1/apoderado/hijos` | `APODERADO` | Sus representados con su sección |
| `/api/v1/demo/...` | autenticado | Endpoints de comprobación de permisos por rol |

**Ejemplo de flujo** (Swagger UI hace esto interactivo):

1. `POST /api/v1/admin/periodos` → crear el período y `PATCH /{id}/estado` a `ABIERTO` (sólo puede haber uno vigente).
2. `POST /api/v1/admin/secciones` → crear la sección ("3°A").
3. `POST /api/v1/secretaria/estudiantes` y `POST /api/v1/secretaria/apoderados` → alta de alumno y familia.
4. `POST /api/v1/secretaria/apoderados/{id}/representados` → vincular al apoderado con su hijo.
5. `POST /api/v1/secretaria/matriculas` → inscribir al alumno en la sección (nace `PENDIENTE`; `PATCH /{id}/estado` a `PAGADO` cuando se confirme).
6. `GET /api/v1/apoderado/hijos` → el apoderado ve a su hijo con la sección del período vigente.

### Errores

Respuestas de error uniformes en JSON:

```json
{
  "timestamp": "2026-10-14T10:00:00",
  "status": 409,
  "error": "Conflict",
  "message": "El email ya está registrado",
  "path": "/api/v1/auth/register",
  "fields": null
}
```

Códigos habituales: `400` validación, `401` sin token, `403` sin permiso para la zona, `404` inexistente, `409` conflicto de regla de negocio, `429` login bloqueado por intentos fallidos (con `Retry-After`).

---

## Configuración

Variables principales (el completo está en [`.env.example`](.env.example)):

| Variable | Descripción | Por defecto |
|---|---|---|
| `POSTGRES_DB/USER/PASSWORD` | Credenciales de la base de datos | — |
| `POSTGRES_PORT` / `BACKEND_PORT` | Puertos del host | `5432` / `8080` |
| `APP_JWT_SECRET` | Secreto del JWT (mínimo 32 bytes, obligatorio) | — |
| `APP_JWT_EXPIRATION_MS` | Validez del token | `86400000` (24 h) |
| `APP_CORS_ALLOWED_ORIGINS` | Orígenes del SPA permitidos (coma) | `*` |
| `APP_SECURITY_LOGIN_MAX_ATTEMPTS` | Intentos de login antes de bloquear | `5` |
| `APP_SECURITY_LOGIN_WINDOW_MINUTES` | Ventana de los intentos | `15` |
| `APP_SECURITY_LOGIN_COOLDOWN_MINUTES` | Bloqueo tras superar el límite | `15` |

---

## Estructura

```
src/main/java/com/educa360/backend/
├── controller/   # Endpoints REST (una zona por clase)
├── service/      # Reglas de negocio y transacciones
├── repository/   # Spring Data JPA
├── entity/       # JPA: User, Docente, Estudiante, Apoderado, Materia,
│                 # Seccion, Curso, HorarioBloque, Matricula, PeriodoAcademico...
├── dto/          # Requests con validación y Responses por pantalla
├── security/     # JWT, filtros y configuración de seguridad
└── exception/    # Excepciones de negocio + manejo global (JSON uniforme)
```

---

## Pruebas

```bash
./mvnw test      # 79 pruebas: unitarias + integración (Testcontainers)
```

Cada clase de integración levanta su propia PostgreSQL 17 efímera: las pruebas **nunca tocan la base de datos de desarrollo** y se saltan si no hay Docker. El CI de GitHub Actions las ejecuta en cada push.
