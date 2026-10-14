package com.educa360.backend;

import com.educa360.backend.repository.ApoderadoRepository;
import com.educa360.backend.repository.CursoRepository;
import com.educa360.backend.repository.DocenteRepository;
import com.educa360.backend.repository.EstudianteRepository;
import com.educa360.backend.repository.HorarioBloqueRepository;
import com.educa360.backend.repository.MateriaRepository;
import com.educa360.backend.repository.PeriodoAcademicoRepository;
import com.educa360.backend.repository.SeccionRepository;
import com.educa360.backend.repository.SecretariaRepository;
import com.educa360.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * Tests del Módulo 2: catálogo académico (períodos, materias, secciones,
 * habilitaciones, cursos y horarios).
 *
 * Reglas que protege el suite:
 * - sólo UN período ABIERTO a la vez;
 * - "grado+letra" único por período (secundaria: grado 1..5);
 * - único curso por (materia, sección);
 * - sin habilitación previa del docente no se crea el curso;
 * - materia desactivada frena nuevos cursos;
 * - el horario no solapa a la sección ni al docente.
 *
 * PostgreSQL efímera (Testcontainers): no toca la BD de desarrollo.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@AutoConfigureMockMvc
class CatalogoAcademicoApiTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    private static final Pattern TOKEN_PATTERN = Pattern.compile("\"token\":\"([^\"]+)\"");

    private static final String PERIODOS = "/api/v1/admin/periodos";
    private static final String MATERIAS = "/api/v1/admin/materias";
    private static final String SECCIONES = "/api/v1/admin/secciones";
    private static final String CURSOS = "/api/v1/admin/cursos";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DocenteRepository docenteRepository;

    @Autowired
    private MateriaRepository materiaRepository;

    @Autowired
    private PeriodoAcademicoRepository periodoRepository;

    @Autowired
    private SeccionRepository seccionRepository;

    @Autowired
    private CursoRepository cursoRepository;

    @Autowired
    private HorarioBloqueRepository horarioRepository;

    @Autowired
    private EstudianteRepository estudianteRepository;

    @Autowired
    private SecretariaRepository secretariaRepository;

    @Autowired
    private ApoderadoRepository apoderadoRepository;

    private String admin;

    @BeforeEach
    void empezarDeCero() throws Exception {
        // Orden de borrado por las FK: lo hijo primero
        horarioRepository.deleteAll();
        cursoRepository.deleteAll();
        seccionRepository.deleteAll();
        docenteRepository.deleteAll(); // con ella se van las filas de la join docente_materia
        materiaRepository.deleteAll();
        periodoRepository.deleteAll();
        estudianteRepository.deleteAll(); // el test de RBAC crea uno
        secretariaRepository.deleteAll();
        apoderadoRepository.deleteAll();
        userRepository.deleteAll();

        // Alta inicial: el primer usuario de la BD siempre es ADMIN
        admin = registrarAdmin();
    }

    // ==================== Períodos ====================

    @Test
    @DisplayName("El período nace CERRADO y sólo se abre con PATCH /estado")
    void periodoNaceCerradoYSeAbre() throws Exception {
        MvcResult creado = crear(PERIODOS, """
                {"nombre":"Bimestre I, 2026","fechaInicio":"2026-03-02","fechaFin":"2026-07-17"}""");
        assertEquals(201, creado.getResponse().getStatus());
        String cuerpo = creado.getResponse().getContentAsString();
        assertTrue(cuerpo.contains("\"estado\":\"CERRADO\""), "debe nacer cerrado: " + cuerpo);

        long id = idDe(creado);
        MvcResult abierto = conToken(patch(PERIODOS + "/" + id + "/estado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"ABIERTO\"}"), admin);
        assertEquals(200, abierto.getResponse().getStatus());
        assertTrue(abierto.getResponse().getContentAsString().contains("\"estado\":\"ABIERTO\""));
    }

    @Test
    @DisplayName("Fechas invertidas (fin antes que inicio) se rechazan con 400")
    void fechasInvertidasResponde400() throws Exception {
        MvcResult resultado = crear(PERIODOS, """
                {"nombre":"Raro","fechaInicio":"2026-07-17","fechaFin":"2026-03-02"}""");

        assertEquals(400, resultado.getResponse().getStatus());
    }

    @Test
    @DisplayName("Sólo UN período abierto a la vez: abrir otro con vigente responde 409")
    void soloUnPeriodoAbiertoALaVez() throws Exception {
        long primero = crearPeriodo("Bimestre I, 2026", "2026-03-02", "2026-07-17");
        long segundo = crearPeriodo("Bimestre II, 2026", "2026-08-03", "2026-12-18");

        assertEquals(200, cambiarEstado(primero, "ABIERTO"));
        assertEquals(409, cambiarEstado(segundo, "ABIERTO"), "no puede haber dos vigentes");

        assertEquals(200, cambiarEstado(primero, "CERRADO"), "cerrar siempre es válido");
        assertEquals(200, cambiarEstado(segundo, "ABIERTO"), "al cerrar el uno, el otro ya puede abrir");
    }

    @Test
    @DisplayName("Dos períodos con el mismo nombre se rechazan con 409")
    void nombreDePeriodoDuplicadoResponde409() throws Exception {
        crearPeriodo("Bimestre I, 2026", "2026-03-02", "2026-07-17");
        MvcResult repetido = crear(PERIODOS, """
                {"nombre":"Bimestre I, 2026","fechaInicio":"2027-03-02","fechaFin":"2027-07-17"}""");

        assertEquals(409, repetido.getResponse().getStatus());
    }

    // ==================== Secciones ====================

    @Test
    @DisplayName("Grado fuera de 1..5 o letra inválida responden 400 (sólo secundaria)")
    void gradoFueraDeRangoResponde400() throws Exception {
        long periodo = crearPeriodo("2026", "2026-03-02", "2026-12-18");

        assertEquals(400, estadoDeSeccion(6, "A", periodo, null), "grado 6 no existe en secundaria");
        assertEquals(400, estadoDeSeccion(0, "A", periodo, null));
        assertEquals(400, estadoDeSeccion(1, "a", periodo, null), "la letra va en mayúscula");
    }

    @Test
    @DisplayName("La sección trae etiqueta, período y tutor en su respuesta")
    void crearSeccionConTutor() throws Exception {
        long periodo = crearPeriodo("2026", "2026-03-02", "2026-12-18");
        long tutor = crearDocente("tutor@educa360.pe", "20000010");

        MvcResult resultado = crear(SECCIONES, seccionBody(3, "A", periodo, tutor));

        assertEquals(201, resultado.getResponse().getStatus());
        String cuerpo = resultado.getResponse().getContentAsString();
        assertTrue(cuerpo.contains("\"grado\":3"));
        assertTrue(cuerpo.contains("\"letra\":\"A\""));
        assertTrue(cuerpo.contains("\"aula\":\"A-101\""));
        assertTrue(cuerpo.contains("\"periodoNombre\":\"2026\""));
        assertTrue(cuerpo.contains("\"tutorNombre\":\"Dana Perez\""), "el nombre del tutor debe viajar: " + cuerpo);
    }

    @Test
    @DisplayName("'grado+letra' es único DENTRO de un período; en otro período sí se puede repetir")
    void seccionDuplicadaEnElPeriodoResponde409() throws Exception {
        long bimestre1 = crearPeriodo("Bimestre I", "2026-03-02", "2026-07-17");
        long bimestre2 = crearPeriodo("Bimestre II", "2026-08-03", "2026-12-18");
        assertEquals(201, estadoDeSeccion(3, "A", bimestre1, null));

        assertEquals(409, estadoDeSeccion(3, "A", bimestre1, null), "misma sección repetida en el mismo período");
        assertEquals(201, estadoDeSeccion(3, "A", bimestre2, null), "las secciones se re-forman cada período");
    }

    @Test
    @DisplayName("Una sección con período inexistente responde 404")
    void seccionConPeriodoInexistenteResponde404() throws Exception {
        assertEquals(404, estadoDeSeccion(1, "A", 999L, null));
    }

    // ==================== Materias y habilitaciones ====================

    @Test
    @DisplayName("Dos materias con el mismo nombre se rechazan con 409")
    void materiaDuplicadaResponde409() throws Exception {
        crearMateria("Matematicas"); // la primera sí se crea (exige 201)
        MvcResult repetida = crear(MATERIAS, "{\"nombre\":\"Matematicas\"}");

        assertEquals(409, repetida.getResponse().getStatus());
    }

    @Test
    @DisplayName("PUT de habilitaciones reemplaza el conjunto completo de materias")
    void habilitarMateriasReemplazaElConjunto() throws Exception {
        long docente = crearDocente("doc1@educa360.pe", "20000011");
        long matematicas = crearMateria("Matematicas");
        long fisica = crearMateria("Fisica");

        assertEquals(200, habilitar(docente, "[%d,%d]".formatted(matematicas, fisica)));
        String ambas = listarMaterias(docente);
        assertTrue(ambas.contains("Matematicas") && ambas.contains("Fisica"));

        assertEquals(200, habilitar(docente, "[%d]".formatted(matematicas)), "un PUT pinta la hoja entera");
        String soloUna = listarMaterias(docente);
        assertTrue(soloUna.contains("Matematicas"));
        assertFalse(soloUna.contains("Fisica"), "las que no vinieron se quitan");

        assertEquals(404, habilitar(docente, "[999]"), "materia inexistente");
    }

    // ==================== Cursos ====================

    @Test
    @DisplayName("Crear un curso para un docente SIN la materia habilitada responde 409")
    void cursoSinHabilitacionResponde409() throws Exception {
        long docente = crearDocente("doc1@educa360.pe", "20000011");
        long matematicas = crearMateria("Matematicas");
        long periodo = crearPeriodo("2026", "2026-03-02", "2026-12-18");
        long seccion = nuevaSeccion(1, "A", periodo);

        assertEquals(409, curso(docente, matematicas, seccion),
                "sin habilitación previa no se crea el curso");
    }

    @Test
    @DisplayName("Una materia sólo puede tener UN curso por sección (409 al repetir)")
    void cursoUnicoPorMateriaYSeccion() throws Exception {
        long dana = crearDocente("doc1@educa360.pe", "20000011");
        long elva = crearDocente("doc2@educa360.pe", "20000012");
        long matematicas = crearMateria("Matematicas");
        long fisica = crearMateria("Fisica");
        long periodo = crearPeriodo("2026", "2026-03-02", "2026-12-18");
        long seccion = nuevaSeccion(3, "A", periodo);

        habilitar(dana, "[%d,%d]".formatted(matematicas, fisica));
        habilitar(elva, "[%d]".formatted(fisica));

        assertEquals(201, curso(dana, matematicas, seccion));
        assertEquals(409, curso(dana, matematicas, seccion), "misma materia y sección");

        assertEquals(201, curso(elva, fisica, seccion), "otra materia en la misma sección sí");
        assertEquals(409, curso(dana, fisica, seccion), "la materia ya tiene curso en esa sección");
    }

    @Test
    @DisplayName("Una materia desactivada frena NUEVOS cursos")
    void materiaDesactivadaBloqueaNuevosCursos() throws Exception {
        long dana = crearDocente("doc1@educa360.pe", "20000011");
        long matematicas = crearMateria("Matematicas");
        long periodo = crearPeriodo("2026", "2026-03-02", "2026-12-18");
        long seccion3A = nuevaSeccion(3, "A", periodo);
        long seccion4B = nuevaSeccion(4, "B", periodo);
        habilitar(dana, "[%d]".formatted(matematicas));

        assertEquals(201, curso(dana, matematicas, seccion3A), "habilitada y activa: pasa");

        MvcResult desactivada = conToken(patch(MATERIAS + "/" + matematicas + "/activa")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"activa\":false}"), admin);
        assertEquals(200, desactivada.getResponse().getStatus());

        assertEquals(409, curso(dana, matematicas, seccion4B),
                "reactivar la materia antes de crear el curso");
    }

    @Test
    @DisplayName("Eliminar un curso y repetir el borrado responde 404")
    void eliminarCursoYRepetirResponde404() throws Exception {
        long dana = crearDocente("doc1@educa360.pe", "20000011");
        long matematicas = crearMateria("Matematicas");
        long periodo = crearPeriodo("2026", "2026-03-02", "2026-12-18");
        long seccion = nuevaSeccion(1, "A", periodo);
        habilitar(dana, "[%d]".formatted(matematicas));
        long cursoId = nuevoCurso(dana, matematicas, seccion);

        MvcResult borrado = conToken(delete(CURSOS + "/" + cursoId), admin);
        assertEquals(204, borrado.getResponse().getStatus());

        assertEquals(404, conToken(delete(CURSOS + "/" + cursoId), admin)
                .getResponse().getStatus(), "ya no existe");
    }

    // ==================== Horario ====================

    @Test
    @DisplayName("El horario se pinta por hoja entera: el PUT reemplaza los bloques")
    void horarioReemplazaLaHojaEntera() throws Exception {
        long cursoId = cursoDeUnDocente("doc1@educa360.pe", "20000011", "Matematicas");

        assertEquals(200, putHorario(cursoId, """
                {"bloques":[{"dia":"LUNES","horaInicio":"08:00","horaFin":"09:00"},
                             {"dia":"MARTES","horaInicio":"08:00","horaFin":"09:00"}]}"""));
        String dos = getHorario(cursoId);
        assertTrue(dos.contains("LUNES") && dos.contains("MARTES"));

        assertEquals(200, putHorario(cursoId, """
                {"bloques":[{"dia":"LUNES","horaInicio":"10:00","horaFin":"11:00"}]}"""));
        String uno = getHorario(cursoId);
        assertTrue(uno.contains("10:00"));
        assertFalse(uno.contains("MARTES"), "los bloques que no vinieron se quitan");
    }

    @Test
    @DisplayName("Dos cursos de la MISMA sección no pueden solaparse (409)")
    void solapeEnLaSeccionResponde409() throws Exception {
        long dana = crearDocente("doc1@educa360.pe", "20000011");
        long elva = crearDocente("doc2@educa360.pe", "20000012");
        long matematicas = crearMateria("Matematicas");
        long fisica = crearMateria("Fisica");
        long periodo = crearPeriodo("2026", "2026-03-02", "2026-12-18");
        long seccion = nuevaSeccion(3, "A", periodo);
        habilitar(dana, "[%d]".formatted(matematicas));
        habilitar(elva, "[%d]".formatted(fisica));
        long cursoMatematicas = nuevoCurso(dana, matematicas, seccion);
        long cursoFisica = nuevoCurso(elva, fisica, seccion);

        assertEquals(200, putHorario(cursoMatematicas, """
                {"bloques":[{"dia":"LUNES","horaInicio":"08:00","horaFin":"09:00"}]}"""));
        assertEquals(409, putHorario(cursoFisica, """
                {"bloques":[{"dia":"LUNES","horaInicio":"08:30","horaFin":"09:30"}]}"""),
                "la sección ya está ocupada a esa hora");
        assertEquals(200, putHorario(cursoFisica, """
                {"bloques":[{"dia":"LUNES","horaInicio":"09:00","horaFin":"10:00"}]}"""),
                "consecutivo (09:00 fin / 09:00 inicio) no es solape");
    }

    @Test
    @DisplayName("El mismo docente no puede estar en dos cursos a la misma hora (409)")
    void solapeEnElDocenteResponde409() throws Exception {
        long dana = crearDocente("doc1@educa360.pe", "20000011");
        long matematicas = crearMateria("Matematicas");
        long periodo = crearPeriodo("2026", "2026-03-02", "2026-12-18");
        long seccion3A = nuevaSeccion(3, "A", periodo);
        long seccion4B = nuevaSeccion(4, "B", periodo);
        habilitar(dana, "[%d]".formatted(matematicas));
        long en3A = nuevoCurso(dana, matematicas, seccion3A);
        long en4B = nuevoCurso(dana, matematicas, seccion4B); // misma materia, otra sección: permitido

        assertEquals(200, putHorario(en3A, """
                {"bloques":[{"dia":"MARTES","horaInicio":"10:00","horaFin":"11:00"}]}"""));
        assertEquals(409, putHorario(en4B, """
                {"bloques":[{"dia":"MARTES","horaInicio":"10:30","horaFin":"11:30"}]}"""),
                "el docente no puede dar dos clases a la vez");
        assertEquals(200, putHorario(en4B, """
                {"bloques":[{"dia":"MARTES","horaInicio":"11:00","horaFin":"12:00"}]}"""));
    }

    @Test
    @DisplayName("Re-PUT idéntico de la hoja es 200 (no 409): repetir no cambia nada")
    void rePutIdenticoDelHorarioEsValido() throws Exception {
        long cursoId = cursoDeUnDocente("doc1@educa360.pe", "20000011", "Matematicas");
        String hoja = """
                {"bloques":[{"dia":"LUNES","horaInicio":"08:00","horaFin":"09:00"},
                             {"dia":"MARTES","horaInicio":"08:00","horaFin":"09:00"}]}""";

        assertEquals(200, putHorario(cursoId, hoja));
        MvcResult segunda = conToken(put(CURSOS + "/" + cursoId + "/horario")
                .contentType(MediaType.APPLICATION_JSON)
                .content(hoja), admin);
        assertEquals(200, segunda.getResponse().getStatus(),
                "repetir la hoja idéntica debe ser un no-op de 200: "
                        + segunda.getResponse().getContentAsString());
    }

    @Test
    @DisplayName("Bloque con horas invertidas o día inválido responde 400")
    void horarioInvalidoResponde400() throws Exception {
        long cursoId = cursoDeUnDocente("doc1@educa360.pe", "20000011", "Matematicas");

        assertEquals(400, putHorario(cursoId, """
                {"bloques":[{"dia":"LUNES","horaInicio":"09:00","horaFin":"08:00"}]}"""),
                "fin antes que inicio");
        assertEquals(400, putHorario(cursoId, """
                {"bloques":[{"dia":"DOMINGO","horaInicio":"08:00","horaFin":"09:00"}]}"""),
                "día fuera del enum");
    }

    // ==================== Zonas por rol ====================

    @Test
    @DisplayName("El DOCENTE ve SÓLO sus cursos desde /docente/cursos")
    void docenteVeSoloSusCursos() throws Exception {
        long dana = crearDocente("doc1@educa360.pe", "20000011");
        long elva = crearDocente("doc2@educa360.pe", "20000012");
        long matematicas = crearMateria("Matematicas");
        long fisica = crearMateria("Fisica");
        long periodo = crearPeriodo("2026", "2026-03-02", "2026-12-18");
        long seccion3A = nuevaSeccion(3, "A", periodo);
        long seccion4B = nuevaSeccion(4, "B", periodo);
        habilitar(dana, "[%d]".formatted(matematicas));
        habilitar(elva, "[%d]".formatted(fisica));
        nuevoCurso(dana, matematicas, seccion3A);
        nuevoCurso(elva, fisica, seccion4B);

        String tokenDana = loginYToken("doc1@educa360.pe", "Docente1234");
        String cursos = conToken(get("/api/v1/docente/cursos"), tokenDana)
                .getResponse().getContentAsString();
        assertTrue(cursos.contains("Matematicas"), "ve el suyo: " + cursos);
        assertFalse(cursos.contains("Fisica"), "no ve los cursos del otro docente");

        assertEquals(403, conToken(get("/api/v1/docente/cursos"), admin).getResponse().getStatus(),
                "la zona /docente es sólo para DOCENTE: ADMIN no es docente");
    }

    @Test
    @DisplayName("GET /admin/docentes expone el id de PERFIL con título y materias")
    void listarDocentesExponeElPerfilId() throws Exception {
        long dana = crearDocente("doc1@educa360.pe", "20000011");
        long matematicas = crearMateria("Matematicas");
        habilitar(dana, "[%d]".formatted(matematicas));

        String cuerpo = conToken(get("/api/v1/admin/docentes"), admin)
                .getResponse().getContentAsString();
        assertTrue(cuerpo.contains("\"titulo\":\"Lic. en Matematicas\""), "el título debe viajar: " + cuerpo);
        assertTrue(cuerpo.contains("Matematicas"), "las materias habilitadas deben venir");
        assertTrue(cuerpo.contains("\"id\":%d".formatted(dana)),
                "el front necesita el id de perfil para /admin/docentes/{id}/materias");
    }

    @Test
    @DisplayName("/auth/me trae el perfil del DOCENTE (título y materias) en una sola llamada")
    void meDelDocenteTraeElPerfil() throws Exception {
        long dana = crearDocente("doc1@educa360.pe", "20000011");
        long matematicas = crearMateria("Matematicas");
        habilitar(dana, "[%d]".formatted(matematicas));

        String tokenDana = loginYToken("doc1@educa360.pe", "Docente1234");
        String cuerpo = conToken(get("/api/v1/auth/me"), tokenDana)
                .getResponse().getContentAsString();
        assertTrue(cuerpo.contains("\"rol\":\"DOCENTE\""));
        assertTrue(cuerpo.contains("\"titulo\":\"Lic. en Matematicas\""), "el perfil debe venir: " + cuerpo);
        assertTrue(cuerpo.contains("Matematicas"), "con sus materias habilitadas");

        String meAdmin = conToken(get("/api/v1/auth/me"), admin)
                .getResponse().getContentAsString();
        assertTrue(meAdmin.contains("\"perfil\":null"), "ADMIN no tiene perfil propio todavía: " + meAdmin);
    }

    // ==================== RBAC ====================

    @Test
    @DisplayName("El catálogo es de ADMIN: 401 sin token y 403 con token de ESTUDIANTE")
    void rbacDelCatalogo() throws Exception {
        assertEquals(401, mockMvc.perform(get(PERIODOS)).andReturn().getResponse().getStatus());

        String estudiante = crearYTokenearEstudiante();
        assertEquals(403, conToken(get(PERIODOS), estudiante).getResponse().getStatus());

        assertEquals(200, conToken(get(PERIODOS), admin).getResponse().getStatus());
    }

    // ==================== Helpers ====================

    private MvcResult crear(String url, String body) throws Exception {
        return conToken(post(url).contentType(MediaType.APPLICATION_JSON).content(body), admin);
    }

    private MvcResult conToken(MockHttpServletRequestBuilder peticion, String token) throws Exception {
        return mockMvc.perform(peticion.header("Authorization", "Bearer " + token)).andReturn();
    }

    private String registrarAdmin() throws Exception {
        MvcResult resultado = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"admin@educa360.pe","password":"Admin12345","nombres":"Ana",
                                 "apellidos":"Ruiz","dni":"10000001"}"""))
                .andReturn();
        assertEquals(201, resultado.getResponse().getStatus());
        return tokenDe(resultado.getResponse().getContentAsString());
    }

    private long crearPeriodo(String nombre, String inicio, String fin) throws Exception {
        return idDe(crear(PERIODOS, """
                {"nombre":"%s","fechaInicio":"%s","fechaFin":"%s"}""".formatted(nombre, inicio, fin)));
    }

    private int cambiarEstado(long id, String estado) throws Exception {
        return conToken(patch(PERIODOS + "/" + id + "/estado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"%s\"}".formatted(estado)), admin)
                .getResponse().getStatus();
    }

    /** Crea la sección y devuelve su id (exige 201). */
    private long nuevaSeccion(int grado, String letra, long periodoId) throws Exception {
        return idDe(crear(SECCIONES, seccionBody(grado, letra, periodoId, null)));
    }

    /** Status de la creación, para expectativas 400/404/409. */
    private int estadoDeSeccion(int grado, String letra, long periodoId, Long tutorId) throws Exception {
        return crear(SECCIONES, seccionBody(grado, letra, periodoId, tutorId))
                .getResponse().getStatus();
    }

    private String seccionBody(int grado, String letra, long periodoId, Long tutorId) {
        return """
                {"grado":%d,"letra":"%s","aula":"A-101","tutorId":%s,"periodoId":%d}
                """.formatted(grado, letra, tutorId == null ? "null" : tutorId, periodoId);
    }

    private long crearMateria(String nombre) throws Exception {
        return idDe(crear(MATERIAS, "{\"nombre\":\"%s\"}".formatted(nombre)));
    }

    private long crearDocente(String email, String dni) throws Exception {
        idDe(crear("/api/v1/admin/docentes", """
                {"usuario":{"email":"%s","password":"Docente1234","nombres":"Dana","apellidos":"Perez",
                            "dni":"%s"},
                 "perfil":{"titulo":"Lic. en Matematicas"}}
                """.formatted(email, dni)));
        return docenteRepository.findAll().stream()
                .filter(d -> d.getUser().getEmail().equals(email))
                .findFirst().orElseThrow().getId();
    }

    private int habilitar(long docenteId, String materiaIdsJson) throws Exception {
        return conToken(put("/api/v1/admin/docentes/" + docenteId + "/materias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"materiaIds\":" + materiaIdsJson + "}"), admin)
                .getResponse().getStatus();
    }

    private String listarMaterias(long docenteId) throws Exception {
        return conToken(get("/api/v1/admin/docentes/" + docenteId + "/materias"), admin)
                .getResponse().getContentAsString();
    }

    /** POST de curso; devuelve el status (para expectativas 201/409). */
    private int curso(long docenteId, long materiaId, long seccionId) throws Exception {
        return crear(CURSOS, """
                {"docenteId":%d,"materiaId":%d,"seccionId":%d}"""
                .formatted(docenteId, materiaId, seccionId))
                .getResponse().getStatus();
    }

    /** POST de curso que exige 201 y devuelve el id. */
    private long nuevoCurso(long docenteId, long materiaId, long seccionId) throws Exception {
        return idDe(crear(CURSOS, """
                {"docenteId":%d,"materiaId":%d,"seccionId":%d}"""
                .formatted(docenteId, materiaId, seccionId)));
    }

    /** Monta el camino completo y devuelve el curso: docente + habilitación + materia + sección. */
    private long cursoDeUnDocente(String email, String dni, String materia) throws Exception {
        long docente = crearDocente(email, dni);
        long materiaId = crearMateria(materia);
        long periodo = crearPeriodo("2026", "2026-03-02", "2026-12-18");
        long seccion = nuevaSeccion(3, "A", periodo);
        habilitar(docente, "[%d]".formatted(materiaId));
        return nuevoCurso(docente, materiaId, seccion);
    }

    private int putHorario(long cursoId, String body) throws Exception {
        return conToken(put(CURSOS + "/" + cursoId + "/horario")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body), admin)
                .getResponse().getStatus();
    }

    private String getHorario(long cursoId) throws Exception {
        return conToken(get(CURSOS + "/" + cursoId + "/horario"), admin)
                .getResponse().getContentAsString();
    }

    /** Id de un recurso recién creado: exige que la creación haya sido 201. */
    private long idDe(MvcResult resultado) throws Exception {
        assertEquals(201, resultado.getResponse().getStatus(),
                "creación inesperada: " + resultado.getResponse().getContentAsString());
        return idDeSilencioso(resultado);
    }

    private long idDeSilencioso(MvcResult resultado) throws Exception {
        Matcher matcher = Pattern.compile("\"id\":(\\d+)")
                .matcher(resultado.getResponse().getContentAsString());
        assertTrue(matcher.find(), "la respuesta debe traer el id");
        return Long.parseLong(matcher.group(1));
    }

    private String tokenDe(String cuerpo) {
        Matcher matcher = TOKEN_PATTERN.matcher(cuerpo);
        assertNotNull(matcher.find(), "debe venir el token");
        return matcher.group(1);
    }

    private String crearYTokenearEstudiante() throws Exception {
        MvcResult creado = crear("/api/v1/secretaria/estudiantes", """
                {"usuario":{"email":"alu@educa360.pe","password":"Alumno1234","nombres":"Alma",
                            "apellidos":"Luna","dni":"30000001"},
                 "perfil":{"fechaNacimiento":"2011-03-03"}}""");
        assertEquals(201, creado.getResponse().getStatus());
        return loginYToken("alu@educa360.pe", "Alumno1234");
    }

    private String loginYToken(String email, String password) throws Exception {
        MvcResult resultado = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password)))
                .andReturn();
        assertEquals(200, resultado.getResponse().getStatus());
        return tokenDe(resultado.getResponse().getContentAsString());
    }
}
