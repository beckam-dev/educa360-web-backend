# Contexto del Proyecto: Educa360 (Backend)

## 1. Visión General del Proyecto
- **Nombre:** Educa360
- **Institución objetivo:** Colegio Nuestra Señora de Monserrat (Nivel Secundaria).
- **Propósito:** Sistema web integral para la gestión académica, seguimiento estudiantil en tiempo real y comunicación bidireccional entre la institución, docentes, estudiantes y apoderados.
- **Tipo de aplicación:** API RESTful desacoplada para dar soporte a un cliente SPA (React + Vite).

---

## 2. Stack Tecnológico Backend
- **Lenguaje:** Java 25.
- **Framework:** Spring Boot **4.1.1** (Spring WebMVC, Spring Security, Spring Data JPA).
- **Autenticación y Autorización:** Stateless con JWT (JSON Web Tokens) y RBAC (Control de acceso basado en roles).
- **Base de Datos:** **PostgreSQL 17** — en desarrollo corre en Docker (`docker-compose.yml`); en producción se apunta a Supabase.
- **Almacenamiento de Archivos:** Supabase Storage (Buckets para sustentos en PDF/imágenes y material de clase).
- **Documentación API:** Swagger / OpenAPI 3 (springdoc-openapi).
- **Gestor de Dependencias:** Maven.
- **Arquitectura de Software:** Arquitectura en capa limpia:
  - `controller`: Endpoints REST, validación DTO (`@Valid`).
  - `service`: Lógica de negocio transaccional (`@Transactional`).
  - `repository`: Interfaces Spring Data JPA (`JpaRepository`).
  - `entity`: Modelos JPA / Tablas relacionales. Todas las entidades extienden de `Auditable` (`id`, `createdAt`, `updatedAt`).
  - `dto`: Request/Response records para desacoplar el modelo y no exponer entidades.
  - `security`: `SecurityConfig` (SecurityFilterChain + CORS), `JwtService`, `JwtAuthenticationFilter`, `AppUserDetailsService`.
  - `service`: `AuthService` (login, registro, `/me`) y `LoginAttemptService` (límite de intentos de login).
  - `exception`: `GlobalExceptionHandler` (`@RestControllerAdvice`), `ApiError`, `ConflictException` (409) y `RateLimitedException` (429).
- **Pruebas:** JUnit 5 + MockMvc + **Testcontainers** (PostgreSQL 17 efímera por clase: no toca la BD de desarrollo). `./mvnw test` → 65 tests, ejecutados por el CI (GitHub Actions) en cada push.
- **Entorno:** Docker Compose (`docker compose up -d` → Postgres + backend). Los secretos viven en `.env` (no versionado; su plantilla vacía es `.env.example`) y el backend **no arranca** sin `APP_JWT_SECRET`. Postgres sólo escucha en `127.0.0.1`.

---

## 3. Roles del Sistema (RBAC)
1. `ADMIN` (Coordinación / Dirección):
   - Gestión de usuarios del personal (docentes, secretarias, otros administradores).
   - Apertura y gestión de periodos lectivos (bimestres), cursos, materias, aulas y asignación docente.
   - Definición del contenido institucional (materias, recursos, comunicados globales).
   - Acceso a reportes y métricas analíticas globales.
   - **No** gestiona tickets de justificación: esa responsabilidad está delegada a `SECRETARIA`.
2. `SECRETARIA` (Personal administrativo):
   - Evaluación y resolución de **tickets de justificación de inasistencia** (Aprobar / Rechazar).
   - Registro de estudiantes: creación de ficha y gestión de su estado académico (`ACTIVO`, `TRASLADADO`, `RETIRADO`, `EGRESADO`).
   - Gestión de matrículas y pensiones: cambio de estado a **`PAGADO`** (y los estados que defina el sistema).
   - Soporte operativo del alumnado: consulta y actualización de los datos académicos del alumno.
3. `DOCENTE`:
   - Registro de asistencia por sesión lectiva (Presente, Tardanza, Falta, Falta Justificada).
   - Registro y cálculo de calificaciones en escala vigesimal (0–20).
   - Publicación de recursos didácticos por sesión/semana.
   - Emisión de comunicados específicos para sus secciones o cursos.
4. `ESTUDIANTE`:
   - Consulta de sus propias calificaciones y promedios en tiempo real.
   - Consulta de su historial de asistencias y faltas acumuladas.
   - Descarga de materiales de clase.
   - Visualización de comunicados dirigidos a su aula o generales.
5. `APODERADO`:
   - Supervisión del rendimiento académico y notas vigesimales de su(s) hijo(s).
   - Visualización del registro diario de asistencia para detección temprana de ausentismo.
   - Creación y seguimiento de **Tickets de Justificación de Inasistencia** (adjuntando URL/comprobante de Supabase Storage).
   - Recepción de comunicados institucionales y citaciones.

---

## 4. Modelo de Dominio / Entidades Clave

> **Estado:** ✅ = ya implementada en `entity/` · 📋 = diseñada aquí, pendiente de implementar.

1. ✅ `User` / `Usuario`:
   - `id`, `email` (único y obligatorio), `password` (hasheado con BCrypt, **nunca** expuesto en respuestas JSON), `nombres`, `apellidos`, `dni` (único), `rol` (Enum: `ADMIN`, `SECRETARIA`, `DOCENTE`, `ESTUDIANTE`, `APODERADO`), `activo` (boolean, **única fuente de baja lógica del sistema**), `createdAt`, `updatedAt`.
   - Los perfiles **no** duplican el estado de baja: se desactiva la cuenta, no el perfil.
2. ✅ `Docente`, `Estudiante`, `Apoderado`, `Secretaria`:
   - Perfiles específicos vinculados **1:1** a `User` (un usuario = un perfil).
   - `Estudiante`: `fechaNacimiento`, `estado` (`ACTIVO`, `TRASLADADO`, `RETIRADO`, `EGRESADO`) y método `estaHabilitado()`.
   - `Apoderado` ↔ `Estudiante`: asociación `ApoderadoEstudiante` (N:M con atributos: `parentesco`, `responsablePrincipal`, `activo`).
   - `Telefono`: 1:N **desde `User`** (colección única para todos los perfiles). Lo usan `DOCENTE`, `SECRETARIA`, `APODERADO` y `ADMIN`; `ESTUDIANTE` no, porque al ser menor la comunicación se canaliza por su apoderado.
3. ✅ `PeriodoAcademico`:
   - `id`, `nombre` (único; ej. Bimestre I, 2026), `fechaInicio`, `fechaFin`, `estado` (`ABIERTO`/`CERRADO`).
   - **Regla:** sólo puede haber UNO `ABIERTO` a la vez — es el período vigente al que apuntarán asistencia y calificaciones. Nace `CERRADO` y se abre con `PATCH /admin/periodos/{id}/estado`; abrir con otro vigente → `409`.
4. ✅ `Materia` (catálogo académico de la institución):
   - `id`, `nombre` (único), `descripcion`, `activa`.
   - Relación **N:M con `Docente`**: las **materias que un docente puede enseñar** (habilitaciones, con endpoints en `/admin/docentes/{id}/materias`).
   - `activa=false` **impide nuevas asignaciones docentes**; las existentes se conservan (es un archivado, no un borrado).
   - El `nivel`/`grado` no viven en la materia: se definen en `Seccion`.
5. ✅ `Seccion`:
   - `id`, `grado` (acotado a **1..5**: el sistema es sólo secundaria, sin campo `nivel`), `letra` (mayúscula), `aula` (texto libre, p. ej. "A-101" — **sin entidad `Aula`**; se extraerá como tabla si aparecen capacidad u horarios), `tutorId` (docente opcional), `periodoId`.
   - **Regla:** `grado+letra` es único DENTRO de un período — las secciones se re-forman cada período (así asistencia y matrícula quedan acotadas al período sin columnas extra).
6. ✅ `Curso` (antes `AsignacionDocente`) / 📋 `Matricula`:
   - `Curso`: la oferta concreta del período — docente × materia × sección. **Es "el curso" del colegio** (Matemática · 3°A · profe X); `Materia` es el catálogo ("lo que se enseña") y el `Curso` su instancia concreta. Único por (materia, sección); exige materia habilitada al docente y materia activa. El nombre anterior describía la *acción* de asignar; la clase ahora es la identidad que cuelgan asistencia, notas y materiales, y que carga su horario.
   - ✅ `HorarioBloque` (del curso): `cursoId`, `dia` (`LUNES`..`SABADO`), `horaInicio`, `horaFin` — sesión **semanal recurrente** (1:N: un curso se ve varias veces por semana). Sin solapes por sección ni por docente el mismo día.
   - **Limitación de alcance:** no se implementará un módulo completo de matrículas. La `Matricula` se modela como un registro con estados (ej. `PENDIENTE`, `PAGADO`) gestionados por `SECRETARIA` y actualizados por reglas automáticas del sistema.
7. 📋 `Asistencia`:
   - `id`, `estudianteId`, `bloqueId` (el `HorarioBloque` del curso), `fecha`, `estado` (`PRESENTE`, `TARDANZA`, `FALTA`, `JUSTIFICADA`), `observacion`.
   - Se toma **por sesión (bloque)**: el tutor registra a su grupo en el bloque de la hora en curso — de ahí que el horario fuera indispensable.
8. 📋 `Calificacion`:
   - `id`, `estudianteId`, `asignacionId` (materia + sección, y el período viene en la sección), `criterio/competencia`, `nota` (0.0 a 20.0), `fechaRegistro`.
9. 📋 `TicketJustificacion`:
   - `id`, `apoderadoId`, `estudianteId`, `fechaInasistencia`, `motivo`, `urlArchivoSustento`, `estado` (`PENDIENTE`, `APROBADO`, `RECHAZADO`), `fechaResolucion`, `observacionesAdmin`.
   - **Regla de negocio:** Al aprobarse un ticket por la `SECRETARIA`, el registro de `Asistencia` correspondiente debe cambiar automáticamente a `JUSTIFICADA`.
10. 📋 `MaterialClase`:
    - `id`, `asignacionId`, `docenteId`, `titulo`, `urlArchivo`, `semana/bloque`, `fechaPublicacion`.
11. 📋 `Comunicado`:
    - `id`, `autorId`, `titulo`, `contenido`, `alcance` (`GLOBAL`, `GRADO`, `SECCION`), `destinatarioSeccionId` (nullable), `fechaPublicacion`.

---

## 6. Estado de la Implementación

**Hecho y probado:**
- Autenticación y autorización: login JWT, registro con alta inicial, `/me`, RBAC por rol, límite de intentos, errores uniformes en JSON y CORS para el SPA.
- Gestión de usuarios y perfiles: altas por rol con cuenta y perfil creados en la misma transacción, en dos zonas — personal (`/api/v1/admin/{usuarios,docentes,secretarias}`, sólo `ADMIN`) y alumnos/familias (`/api/v1/secretaria/{estudiantes,apoderados}`, `ADMIN` + `SECRETARIA`). Listado con filtros, edición, baja lógica y reseteo de contraseña en `/api/v1/admin/usuarios`. `/auth/register` queda reducido al arranque (sólo con BD vacía): un solo camino de creación, sin duplicidades.
- Catálogo académico (Módulo 2, todo `ADMIN`): períodos (**sólo uno `ABIERTO` a la vez**, nacen `CERRADOS`), materias del catálogo (CRUD + archivar con `activa`), secciones por período (grado 1..5, `grado+letra` único por período, tutor opcional), **habilitaciones** de materia por docente (`GET|PUT /admin/docentes/{id}/materias`, el `PUT` reemplaza el conjunto), **cursos** (`GET|POST|DELETE /admin/cursos` — únicos por materia+sección, exigen habilitación y materia activa; baja física) y su **horario** (`GET|PUT /admin/cursos/{id}/horario`: bloques semanales sin solapes por sección ni por docente).
- Vistas por rol para el front (cierre del Módulo 2): `GET /docente/cursos` (el docente sólo ve los suyos), `GET /admin/docentes` (id de perfil + título + materias, para habilitar) y `/auth/me` con el perfil del rol. Dos bugs corregidos en el camino: el re-PUT idéntico del horario (el unique veía la hoja nueva antes de borrar la vieja; resuelto con `flush()` explícito) y `application.properties` esperaba `APP_LOGIN_*` cuando compose/.env exportan `APP_SECURITY_LOGIN_*` (siempre caían los defaults).
- 13 entidades: `Auditable`, `User`, `Docente`, `Estudiante`, `Apoderado`, `Secretaria`, `Materia`, `Telefono`, `ApoderadoEstudiante`, `PeriodoAcademico`, `Seccion`, `Curso`, `HorarioBloque`.
- 65 tests (unitarios + integración con Testcontainers) ejecutándose en el CI de GitHub Actions en cada push.
- Docker Compose (Postgres + backend) con secretos fuera de Git y `.env.example` para el equipo.
- Documentación interactiva en Swagger: `http://localhost:8080/swagger-ui/index.html`.

**Pendiente (por módulo):**
- Representados de apoderados (`ApoderadoEstudiante`): vendrá con el módulo de matrículas — y con él, `GET /api/v1/apoderado/hijos`.
- Estudiantes y matrículas (`SECRETARIA`): su listado `GET /api/v1/secretaria/estudiantes` y los estados de matrícula. **Ahí nacerá `EstudianteResponse`** (con sección, matrícula y apoderado), junto con la edición acotada de la ficha del alumno; no antes, y sin reutilizar `UsuarioResponse` para ese listado — es la vista de cuentas de ADMIN, no la de alumnos de Secretaría. Con la matrícula en pie se desbloquean además `POST /api/v1/secretaria/matriculas`, `GET /api/v1/estudiante/cursos` (los de su sección) y el `perfil` de ESTUDIANTE/APODERADO en `/auth/me` — todo lo que el front necesita para desmockear la zona de familias.
- Asistencia diaria y calificaciones vigesimales.
- Tickets de justificación (apoderado → secretaria) y su efecto automático sobre la asistencia.
- Materiales de clase y comunicados.
- Archivos en Supabase Storage (sustentos y material didáctico).
- Endurecimiento, antes de producción: Flyway + perfil `dev`, refresh tokens, Docker non-root, CI en `main`.

**Cómo se ejecuta:**
- `docker compose up -d` → Postgres (sólo accesible en `127.0.0.1`) + backend en `http://localhost:8080`.
- `./mvnw spring-boot:run` → desarrollo con recarga; usa el mismo Postgres de Docker.
- `./mvnw test` → 65 tests contra una PostgreSQL efímera (requiere Docker; sin él se saltan).
- Alta inicial: la primera cuenta se crea con `POST /api/v1/auth/register` (sólo con BD vacía) y queda como `ADMIN`; en cuanto hay una, ese endpoint se cierra y todo lo demás se gestiona en `/api/v1/admin/...`.

---

## 5. Módulos y Reglas de Negocio Esenciales
- **Autenticación:**
  - `POST /api/v1/auth/login` (`{email, password}`) → `200` con `{token, tokenType: "Bearer", expiresIn (segundos), user}`.
  - **Límite de intentos (anti fuerza bruta):** 5 fallos dentro de 15 minutos bloquean el login durante 15 minutos (medidos desde el último fallo) → `429` con header `Retry-After`. Un acierto reinicia el contador y la clave es `email+IP`, para que el que queda bloqueado sea el que falla y no la víctima. Se comprueba antes de validar la contraseña. El estado vive en memoria (se pierde al reiniciar; con varias instancias haría falta Redis). Configurable con `app.security.login.*`.
  - `POST /api/v1/auth/register` → **arranque del sistema** (`UsuarioBasicoRequest`: email, contraseña, nombres, apellidos, DNI, teléfonos). Funciona **únicamente con la BD vacía**: cualquiera crea la primera cuenta y nace **siempre `ADMIN`** (si naciera con otro rol, el sistema quedaría bloqueado, ya que a partir de ahí sólo un ADMIN podría crear cuentas; el "truco" además neutraliza la condición de carrera de `count()`, porque el peor caso son dos ADMIN, nunca un sistema sin administradores). En cuanto existe una sola cuenta, responde `403` — **incluso con token de ADMIN** — y todas las altas pasan a `/api/v1/admin/{usuarios,docentes,estudiantes,secretarias,apoderados}`, donde cada servicio crea la cuenta **y su perfil** con los datos que el rol exige. Un solo interruptor + una sola zona de gestión = sin caminos paralelos. Todas las altas pasan por `UsuarioService.crearCuenta()` → una sola verdad (unicidad de email/DNI, hash BCrypt, teléfonos) y ninguna cuenta a medias. El email se normaliza a minúsculas y el DNI debe tener 8 dígitos.
  - `GET /api/v1/auth/me` → la cuenta + el perfil de su rol en **UNA llamada** (`{usuario, perfil}`): hoy el `DOCENTE` trae título y materias habilitadas; el `ESTUDIANTE` añadirá su sección y el `APODERADO` sus hijos con el Módulo 3 (los demás roles, `perfil: null`).
  - El cliente envía `Authorization: Bearer <token>` en cada petición. API **stateless**: sin sesiones ni cookies.
  - Errores en JSON (`ApiError`, sin stack traces): `400` validación de DTO con detalle campo a campo o cuerpo ilegible, `401` credenciales inválidas o token ausente, `403` usuario inactivo o sin permiso, `404`/`405`/`415` para ruta, método o `Content-Type` incorrectos, `409` email/DNI duplicado, `429` límite de intentos de login, `500` sólo para fallos inesperados.
  - `AppCORS`: orígenes del SPA por propiedad `app.cors.allowed-origins` (sin credenciales: la auth va en el header).
  - Validación de roles mediante `@PreAuthorize("hasRole('ADMIN')")`, `@PreAuthorize("hasRole('SECRETARIA')")`, `@PreAuthorize("hasRole('DOCENTE')")`, etc.
  - Endpoints de prueba de RBAC: `GET /api/v1/demo/{admin|secretaria|docente|estudiante|apoderado}` y `/api/v1/demo/authenticated`.
- **Gestión de usuarios y perfiles:** cada rol se da de alta en **su endpoint**, con la forma `{"usuario": {...}, "perfil": {...}}`: la cuenta (`UsuarioBasicoRequest`, común a todos) y los datos propios del perfil (`DocenteRequest`, `EstudianteRequest`, `SecretariaRequest`, `ApoderadoRequest`) van **separados**, de modo que un perfil puede reusarse en otros módulos sin arrastrar credenciales. Convenio de autorización: **la ruta nombra el dominio responsable y el `@PreAuthorize` dice quién tiene permiso** (`ADMIN` es siempre superconjunto).
  - **Personal — sólo `ADMIN`** (`/api/v1/admin/...`):
    - `POST /api/v1/admin/usuarios` → alta de `ADMIN`, el único rol sin perfil propio.
    - `POST /api/v1/admin/docentes` → crea la cuenta **y** el perfil `Docente` en la misma transacción (`titulo` opcional).
    - `POST /api/v1/admin/secretarias` → hoy con `"perfil": {}` (campos reservados).
  - **Alumnos y familias — `ADMIN` y `SECRETARIA`** (`/api/v1/secretaria/...`):
    - `POST /api/v1/secretaria/estudiantes` → crea cuenta + perfil `Estudiante`; exige `fechaNacimiento` y **rechaza teléfonos** (`400`: la comunicación es con su apoderado). Secretaría es quien recibe a las familias, de ahí que sea su dominio.
    - `POST /api/v1/secretaria/apoderados` → hoy con `"perfil": {}`; sus representados vendrán con el módulo de matrículas.
    - *Pendiente:* que Secretaría pueda **editar** la ficha del alumno y cambiar el estado de su matrícula (módulo 3). Esa edición tendrá que venir **acotada a estudiantes y apoderados**; nunca al listado general de cuentas.
  - **Consistencia perfil ↔ rol garantizada:** el rol lo fija el servicio de cada perfil (no un campo del request), así que no puede existir un `User` cuyo rol no corresponda a su perfil. El rol **no es editable**: para cambiarlo se da de baja la cuenta y se crea otra.
  - Email/DNI duplicados → `409`; la comprobación vive en un solo punto (`UsuarioService.crearCuenta()`), por el que pasan **todas** las altas, incluida `/auth/register`.
  - `GET /api/v1/admin/usuarios?rol=&activo=&q=` → listado con filtros combinables; `q` busca por email, nombres, apellidos o DNI. `GET /api/v1/admin/usuarios/{id}` → detalle (`404` si no existe). **Sólo `ADMIN`**: es el listado de TODAS las cuentas (incluidos administradores y docentes).
  - `PUT /api/v1/admin/usuarios/{id}` → nombres, apellidos, teléfonos (lista vacía = quitar todos) y campos del perfil.
  - `PATCH /api/v1/admin/usuarios/{id}/activo` → baja lógica (`{activo: false}`), que corta el acceso al instante.
  - `PATCH /api/v1/admin/usuarios/{id}/password` → reseteo por `ADMIN` (no invalida los tokens ya emitidos; eso lo hace la baja).
  - Sin token `401`; sin permiso para la zona `403`.
- **Catálogo académico (Módulo 2, exclusivo de `ADMIN`, todo bajo `/api/v1/admin/...`):**
  - **Terminología:** `Materia` = "lo que se enseña" (catálogo: Matemática, Física); `Seccion` = el grupo de un período ("3°A"); `Curso` = la instancia del período (**antes `AsignacionDocente`** — renombrada porque "asignación" describía una acción, y la clase ahora es la identidad que cuelgan asistencia, notas y materiales). Donde el roadmap decía `cursoId` ambiguo, ahora se distingue: `cursoId` (la instancia) o `seccionId` (el grupo).
  - `GET|POST /api/v1/admin/periodos`, `PUT /{id}`, `PATCH /{id}/estado`: el período **nace `CERRADO`** y se abre a propósito (así "quién abrió y cuándo" queda claro y la regla vive en un solo método). **Sólo UNO `ABIERTO` a la vez** (`409` si ya hay otro vigente), nombre único (`409`), fin anterior a inicio (`400`).
  - `GET|POST /api/v1/admin/materias`, `PUT /{id}`, `PATCH /{id}/activa`: nombre único (`409`). `activa=false` **impide nuevas asignaciones** sin tocar las existentes — es un "ya no la damos más", no un borrado.
  - `GET|POST /api/v1/admin/secciones` (`?periodoId=`), `PUT /{id}`: grado acotado a **1..5** (sólo secundaria, sin campo `nivel`), letra en mayúscula, `aula` como **texto** (sin entidad `Aula`: se extraerá si aparecen capacidad u horarios), tutor opcional. `grado+letra` único por período (`409`) y **no se mueve de período** (`409`: se crea otra fila — arrastrarla rompería el histórico por período).
  - `GET|PUT /api/v1/admin/docentes/{id}/materias`: las **habilitaciones** del docente ("qué puede enseñar"). El `PUT` **reemplaza el conjunto completo** (misma filosofía que los teléfonos): se envía la lista entera — o vacía para dejarlo sin materias.
  - `GET /api/v1/admin/docentes`: listado con **id de PERFIL**, título, materias habilitadas y estado (`DocenteResponse`) — la pantalla de habilitaciones necesita el id del perfil, que no sale en `UsuarioResponse`: cada pantalla, su salida.
  - **Zona DOCENTE** — `GET /api/v1/docente/cursos`: **sus** cursos (materia, sección, período). Sin filtros: la identidad sale del token, que no puede falsearse (§5, acceso a datos por perfil). `ADMIN` no entra (`403`: la zona es del dueño de los datos).
  - `GET|POST /api/v1/admin/cursos` (`?seccionId=`, `?docenteId=`), `DELETE /{id}`: el período **no se pide** (viene de la sección). Reglas → `409`: único por (materia, sección) — nadie más da Matemáticas en 3°A —, docente sin la materia habilitada y materia desactivada. La baja es **física** (`DELETE`): es configuración del período, no histórico de personas (eso sí se desactiva, como `User.activo`) — y se lleva por delante su horario.
  - `GET|PUT /api/v1/admin/cursos/{id}/horario`: el horario es **1:N** (`HorarioBloque`, sesión semanal recurrente — un curso se ve varias veces por semana). El `PUT` reemplaza la **hoja entera** (misma filosofía que las habilitaciones; repetir la hoja idéntica es un no-op `200`). Reglas → `400` si un bloque termina antes de empezar; `409` por solapes: ni con otro curso de la misma sección ni con otro del mismo docente el mismo día (los bloques consecutivos no solapan: 09:00 fin / 09:00 inicio). La asistencia futura colgará de (bloque, fecha).
  - **Diferidos, con su disparador escrito:** `Unidad`/contenidos (cuando las notas se agrupen por unidad) y `Aula` como entidad (si aparecen capacidad u horarios institucionales). ~~Horario~~: ya implementado.
- **Flujo de Justificaciones:**
  - Solo el apoderado puede registrar solicitudes de justificación para sus representados.
  - La `SECRETARIA` revisa el sustento; si lo aprueba, se actualiza el estado de la asistencia del estudiante de forma transaccional.
- **Matrículas, Pensiones y Estados:**
  - El registro del estudiante y el cambio de estado de matrícula/pensión a `PAGADO` lo realiza `SECRETARIA`.
  - El sistema aplica reglas automáticas (p. ej. al aprobar un ticket la asistencia pasa a `JUSTIFICADA`, cálculo de estados vencidos).
- **Escala de Calificaciones:**
  - Estrictamente vigesimal: valores entre `0` y `20`.
- **Acceso a Datos por Perfil:**
  - El estudiante solo puede consultar su propia información.
  - El apoderado solo puede consultar los datos de los estudiantes formalmente asignados a su cargo.
  - El docente solo puede calificar y tomar asistencia en los cursos y secciones que tenga formalmente asignados en el periodo vigente.
  - La `SECRETARIA` solo gestiona tickets, registro/estados del alumnado y matrículas; la estructura académica (períodos, materias, secciones, asignaciones) es exclusiva de `ADMIN`.
  - Al ser menores de edad, la comunicación con el `ESTUDIANTE` (citaciones, avisos de falta) se canaliza por su `APODERADO`: el estudiante no registra teléfonos propios.
