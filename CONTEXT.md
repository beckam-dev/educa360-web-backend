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
- **Arquitectura de Software:** Arquitectura en capas limpia:
  - `controllers`: Endpoints REST, validación DTO (`@Valid`).
  - `services`: Lógica de negocio transaccional (`@Transactional`).
  - `repositories`: Interfaces Spring Data JPA (`JpaRepository`).
  - `entities`: Modelos JPA / Tablas relacionales.
  - `dto`: Request/Response records o clases para desacoplar el modelo.
  - `security`: Filtros JWT, UserDetailsService, SecurityFilterChain.
  - `exceptions`: GlobalExceptionHandler (`@RestControllerAdvice`).

---

## 3. Roles del Sistema (RBAC)
1. `ADMIN` (Coordinación / Dirección):
   - Gestión integral de usuarios (alumnos, docentes, apoderados).
   - Apertura y gestión de periodos lectivos (bimestres), cursos, aulas y asignación docente.
   - Evaluación y resolución de tickets de justificación (Aprobar/Rechazar).
   - Acceso a reportes y métricas analíticas globales.
2. `DOCENTE`:
   - Registro de asistencia por sesión lectiva (Presente, Tardanza, Falta, Falta Justificada).
   - Registro y cálculo de calificaciones en escala vigesimal (0–20).
   - Publicación de recursos didácticos por sesión/semana.
   - Emisión de comunicados específicos para sus secciones o cursos.
3. `ESTUDIANTE`:
   - Consulta de sus propias calificaciones y promedios en tiempo real.
   - Consulta de su historial de asistencias y faltas acumuladas.
   - Descarga de materiales de clase.
   - Visualización de comunicados dirigidos a su aula o generales.
4. `APODERADO`:
   - Supervisión del rendimiento académico y notas vigesimales de su(s) hijo(s).
   - Visualización del registro diario de asistencia para detección temprana de ausentismo.
   - Creación y seguimiento de **Tickets de Justificación de Inasistencia** (adjuntando URL/comprobante de Supabase Storage).
   - Recepción de comunicados institucionales y citaciones.

---

## 4. Modelo de Dominio / Entidades Clave
1. `User` / `Usuario`:
   - `id`, `email` (único), `password` (hasheado con BCrypt), `nombres`, `apellidos`, `dni`, `rol` (Enum), `activo` (boolean), `createdAt`.
2. `Docente`, `Estudiante`, `Apoderado`:
   - Perfiles específicos vinculados 1:1 o heredados de `User`.
   - Relación `Apoderado` 1:N `Estudiante` (o N:M para hermanos/familias).
3. `PeriodoAcademico`:
   - `id`, `nombre` (ej. Bimestre I, 2026), `fechaInicio`, `fechaFin`, `estado` (Abierto/Cerrado).
4. `Curso` / `Asignatura`:
   - `id`, `nombre`, `nivel` (Secundaria), `grado` (1ero a 5to).
5. `Seccion` / `Aula`:
   - `id`, `grado`, `letra` (ej. A, B), `tutorId`, `periodoId`.
6. `Matricula` / `AsignacionDocente`:
   - Cruce entre estudiante/sección y docente/curso/sección.
7. `Asistencia`:
   - `id`, `estudianteId`, `cursoId` o `seccionId`, `fecha`, `estado` (`PRESENTE`, `TARDANZA`, `FALTA`, `JUSTIFICADA`), `observacion`.
8. `Calificacion`:
   - `id`, `estudianteId`, `cursoId`, `periodoId`, `criterio/competencia`, `nota` (0.0 a 20.0), `fechaRegistro`.
9. `TicketJustificacion`:
   - `id`, `apoderadoId`, `estudianteId`, `fechaInasistencia`, `motivo`, `urlArchivoSustento`, `estado` (`PENDIENTE`, `APROBADO`, `RECHAZADO`), `fechaResolucion`, `observacionesAdmin`.
   - **Regla de negocio:** Al aprobarse un ticket por el Admin, el registro de `Asistencia` correspondiente debe cambiar automáticamente a `JUSTIFICADA`.
10. `MaterialClase`:
    - `id`, `cursoId`, `seccionId`, `docenteId`, `titulo`, `urlArchivo`, `semana/bloque`, `fechaPublicacion`.
11. `Comunicado`:
    - `id`, `autorId`, `titulo`, `contenido`, `alcance` (`GLOBAL`, `GRADO`, `SECCION`), `destinatarioSeccionId` (nullable), `fechaPublicacion`.

---

## 5. Módulos y Reglas de Negocio Esenciales
- **Autenticación:**
  - Login por `POST /api/v1/auth/login` retornando JWT Bearer Token y los datos del perfil/rol.
  - Validación de roles mediante `@PreAuthorize("hasRole('ADMIN')")`, `@PreAuthorize("hasRole('DOCENTE')")`, etc.
- **Flujo de Justificaciones:**
  - Solo el apoderado puede registrar solicitudes de justificación para sus representados.
  - El Admin revisa el sustento; si lo aprueba, se actualiza el estado de la asistencia del estudiante de forma transaccional.
- **Escala de Calificaciones:**
  - Estrictamente vigesimal: valores entre `0` y `20`.
- **Acceso a Datos por Perfil:**
  - El estudiante solo puede consultar su propia información.
  - El apoderado solo puede consultar los datos de los estudiantes formalmente asignados a su cargo.
  - El docente solo puede calificar y tomar asistencia en los cursos y secciones que tenga formalmente asignados en el periodo vigente.