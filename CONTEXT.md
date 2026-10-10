# Contexto del Proyecto: Educa360 (Backend)

## 1. Visión General del Proyecto
- **Nombre:** Educa360
- **Institución objetivo:** Colegio Nuestra Señora de Monserrat (Nivel Secundaria).
- **Propósito:** Sistema web integral para la gestión académica, seguimiento estudiantil en tiempo real y comunicación bidireccional entre la institución, docentes, estudiantes y apoderados.
- **Tipo de aplicación:** API RESTful desacoplada para dar soporte a un cliente SPA (React + Vite).

---

## 2. Stack Tecnológico Backend
- **Lenguaje:** Java 25.
- **Framework:** Spring Boot (Spring Web, Spring Security, Spring Data JPA).
- **Autenticación y Autorización:** Stateless con JWT (JSON Web Tokens) y RBAC (Control de acceso basado en roles).
- **Base de Datos:** PostgreSQL (alojada en Supabase / Cloud).
- **Almacenamiento de Archivos:** Supabase Storage (Buckets para sustentos en PDF/imágenes y material de clase).
- **Documentación API:** Swagger / OpenAPI 3 (springdoc-openapi).
- **Gestor de Dependencias:** Maven.
- **Arquitectura de Software:** Arquitectura en capa limpia:
  - `controller`: Endpoints REST, validación DTO (`@Valid`).
  - `service`: Lógica de negocio transaccional (`@Transactional`).
  - `repository`: Interfaces Spring Data JPA (`JpaRepository`).
  - `entity`: Modelos JPA / Tablas relacionales. Todas las entidades extienden de `Auditable` (`id`, `createdAt`, `updatedAt`).
  - `dto`: Request/Response records para desacoplar el modelo y no exponer entidades.
  - `security`: `SecurityConfig` (SecurityFilterChain), `JwtService`, `JwtAuthenticationFilter`, `AppUserDetailsService`.
  - `exception`: `GlobalExceptionHandler` (`@RestControllerAdvice`) + `ApiError`.

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
1. `User` / `Usuario`:
   - `id`, `email` (único y obligatorio), `password` (hasheado con BCrypt, **nunca** expuesto en respuestas JSON), `nombres`, `apellidos`, `dni` (único), `rol` (Enum: `ADMIN`, `SECRETARIA`, `DOCENTE`, `ESTUDIANTE`, `APODERADO`), `activo` (boolean, **única fuente de baja lógica del sistema**), `createdAt`, `updatedAt`.
   - Los perfiles **no** duplican el estado de baja: se desactiva la cuenta, no el perfil.
2. `Docente`, `Estudiante`, `Apoderado`, `Secretaria`:
   - Perfiles específicos vinculados **1:1** a `User` (un usuario = un perfil).
   - `Estudiante`: `fechaNacimiento`, `estado` (`ACTIVO`, `TRASLADADO`, `RETIRADO`, `EGRESADO`) y método `estaHabilitado()`.
   - `Apoderado` ↔ `Estudiante`: asociación `ApoderadoEstudiante` (N:M con atributos: `parentesco`, `responsablePrincipal`, `activo`).
   - `Telefono`: 1:N **desde `User`** (colección única para todos los perfiles). Lo usan `DOCENTE`, `SECRETARIA`, `APODERADO` y `ADMIN`; `ESTUDIANTE` no, porque al ser menor la comunicación se canaliza por su apoderado.
3. `PeriodoAcademico`:
   - `id`, `nombre` (ej. Bimestre I, 2026), `fechaInicio`, `fechaFin`, `estado` (Abierto/Cerrado).
4. `Materia` (catálogo académico de la institución):
   - `id`, `nombre` (único), `descripcion`, `activa`.
   - Relación **N:M con `Docente`**: representa las **materias que un docente puede enseñar**.
   - El `nivel`/`grado` no viven en la materia: se definen en `Seccion` / `Aula`.
5. `Seccion` / `Aula`:
   - `id`, `grado`, `letra` (ej. A, B), `tutorId`, `periodoId`.
6. `Matricula` / `AsignacionDocente`:
   - Cruce entre estudiante/sección y docente/materia/sección.
   - **Limitación de alcance:** no se implementará un módulo completo de matrículas. La `Matricula` se modela como un registro con estados (ej. `PENDIENTE`, `PAGADO`) gestionados por `SECRETARIA` y actualizados por reglas automáticas del sistema.
7. `Asistencia`:
   - `id`, `estudianteId`, `cursoId` o `seccionId`, `fecha`, `estado` (`PRESENTE`, `TARDANZA`, `FALTA`, `JUSTIFICADA`), `observacion`.
8. `Calificacion`:
   - `id`, `estudianteId`, `cursoId`, `periodoId`, `criterio/competencia`, `nota` (0.0 a 20.0), `fechaRegistro`.
9. `TicketJustificacion`:
   - `id`, `apoderadoId`, `estudianteId`, `fechaInasistencia`, `motivo`, `urlArchivoSustento`, `estado` (`PENDIENTE`, `APROBADO`, `RECHAZADO`), `fechaResolucion`, `observacionesAdmin`.
   - **Regla de negocio:** Al aprobarse un ticket por la `SECRETARIA`, el registro de `Asistencia` correspondiente debe cambiar automáticamente a `JUSTIFICADA`.
10. `MaterialClase`:
    - `id`, `cursoId`, `seccionId`, `docenteId`, `titulo`, `urlArchivo`, `semana/bloque`, `fechaPublicacion`.
11. `Comunicado`:
    - `id`, `autorId`, `titulo`, `contenido`, `alcance` (`GLOBAL`, `GRADO`, `SECCION`), `destinatarioSeccionId` (nullable), `fechaPublicacion`.

---

## 5. Módulos y Reglas de Negocio Esenciales
- **Autenticación:**
  - `POST /api/v1/auth/login` (`{email, password}`) → `200` con `{token, tokenType: "Bearer", expiresIn (segundos), user}`.
  - **Límite de intentos (anti fuerza bruta):** 5 fallos dentro de 15 minutos bloquean el login durante 15 minutos (medidos desde el último fallo) → `429` con header `Retry-After`. Un acierto reinicia el contador y la clave es `email+IP`, para que el que queda bloqueado sea el que falla y no la víctima. Se comprueba antes de validar la contraseña. El estado vive en memoria (se pierde al reiniciar; con varias instancias haría falta Redis). Configurable con `app.security.login.*`.
  - `POST /api/v1/auth/register` → alta de cuenta. **Alta inicial (bootstrapping):** si la BD no tiene ninguna cuenta, cualquiera puede crear la primera y su rol es **siempre `ADMIN`** (se ignora el `rol` del request para no dejar el sistema sin administradores); a partir de ahí sólo un `ADMIN` autenticado puede crear cuentas y ahí sí se respeta el rol pedido. El email se normaliza a minúsculas y el DNI debe tener 8 dígitos.
  - `GET /api/v1/auth/me` → perfil del usuario contenido en el token.
  - El cliente envía `Authorization: Bearer <token>` en cada petición. API **stateless**: sin sesiones ni cookies.
  - Errores en JSON (`ApiError`, sin stack traces): `400` validación de DTO con detalle campo a campo o cuerpo ilegible, `401` credenciales inválidas o token ausente, `403` usuario inactivo o sin permiso, `404`/`405`/`415` para ruta, método o `Content-Type` incorrectos, `409` email/DNI duplicado, `429` límite de intentos de login, `500` sólo para fallos inesperados.
  - `AppCORS`: orígenes del SPA por propiedad `app.cors.allowed-origins` (sin credenciales: la auth va en el header).
  - Validación de roles mediante `@PreAuthorize("hasRole('ADMIN')")`, `@PreAuthorize("hasRole('SECRETARIA')")`, `@PreAuthorize("hasRole('DOCENTE')")`, etc.
  - Endpoints de prueba de RBAC: `GET /api/v1/demo/{admin|secretaria|docente|estudiante|apoderado}` y `/api/v1/demo/authenticated`.
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
  - La `SECRETARIA` solo gestiona tickets, registro/estados del alumnado y matrículas; la estructura académica (periodos, cursos, materias, aulas, asignaciones) es exclusiva de `ADMIN`.
  - Al ser menores de edad, la comunicación con el `ESTUDIANTE` (citaciones, avisos de falta) se canaliza por su `APODERADO`: el estudiante no registra teléfonos propios.
