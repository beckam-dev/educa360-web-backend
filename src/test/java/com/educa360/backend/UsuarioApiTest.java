package com.educa360.backend;

import com.educa360.backend.repository.ApoderadoRepository;
import com.educa360.backend.repository.DocenteRepository;
import com.educa360.backend.repository.EstudianteRepository;
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
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * Tests del módulo de gestión de usuarios y perfiles.
 * PostgreSQL efímera (Testcontainers): no toca la BD de desarrollo.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@AutoConfigureMockMvc
class UsuarioApiTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    private static final Pattern TOKEN_PATTERN = Pattern.compile("\"token\":\"([^\"]+)\"");

    private static final String RUTA = "/api/v1/admin/usuarios";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DocenteRepository docenteRepository;

    @Autowired
    private EstudianteRepository estudianteRepository;

    @Autowired
    private SecretariaRepository secretariaRepository;

    @Autowired
    private ApoderadoRepository apoderadoRepository;

    private String admin;

    @BeforeEach
    void empezarDeCero() throws Exception {
        // Perfiles primero: tienen FK hacia users
        docenteRepository.deleteAll();
        estudianteRepository.deleteAll();
        secretariaRepository.deleteAll();
        apoderadoRepository.deleteAll();
        userRepository.deleteAll();

        // Alta inicial: el primer usuario de la BD siempre es ADMIN
        admin = registrarAdmin();
    }

    // ==================== Altas ====================

    @Test
    @DisplayName("Crear un DOCENTE crea su perfil automáticamente con el título")
    void crearDocenteCreaSuPerfil() throws Exception {
        MvcResult resultado = crear(docenteBody("doc1@educa360.pe", "20000001"), admin);

        assertEquals(201, resultado.getResponse().getStatus());
        String cuerpo = resultado.getResponse().getContentAsString();
        assertTrue(cuerpo.contains("\"rol\":\"DOCENTE\""));
        assertTrue(cuerpo.contains("Lic. en Matematicas"), "el título debe viajar en la respuesta");
        assertEquals(1, docenteRepository.count(), "debe existir el perfil Docente");
    }

    @Test
    @DisplayName("Un ESTUDIANTE exige fecha de nacimiento")
    void estudianteSinFechaNacimientoResponde400() throws Exception {
        String body = """
                {"email":"alu@educa360.pe","password":"Alumno1234","nombres":"Alma","apellidos":"Luna",
                 "dni":"30000001","rol":"ESTUDIANTE"}""";

        MvcResult resultado = crear(body, admin);

        assertEquals(400, resultado.getResponse().getStatus());
        assertTrue(resultado.getResponse().getContentAsString().contains("fecha de nacimiento"));
    }

    @Test
    @DisplayName("Un ESTUDIANTE no puede tener teléfonos: la comunicación es con su apoderado")
    void estudianteConTelefonosResponde400() throws Exception {
        String body = """
                {"email":"alu2@educa360.pe","password":"Alumno1234","nombres":"Alma","apellidos":"Luna",
                 "dni":"30000002","rol":"ESTUDIANTE","fechaNacimiento":"2010-05-12","telefonos":["987654321"]}""";

        MvcResult resultado = crear(body, admin);

        assertEquals(400, resultado.getResponse().getStatus());
        assertTrue(resultado.getResponse().getContentAsString().contains("apoderado"));
    }

    @Test
    @DisplayName("Email o DNI ya usados: 409")
    void duplicadosResponde409() throws Exception {
        assertEquals(201, crear(docenteBody("dup@educa360.pe", "40000001"), admin).getResponse().getStatus());

        assertEquals(409, crear(docenteBody("dup@educa360.pe", "40000009"), admin).getResponse().getStatus());
        assertEquals(409, crear(docenteBody("otro@educa360.pe", "40000001"), admin).getResponse().getStatus());
    }

    // ==================== Autorización ====================

    @Test
    @DisplayName("Sólo ADMIN puede gestionar usuarios")
    void rbacDelModulo() throws Exception {
        String tokenEstudiante = crearYTokenear("""
                {"email":"alu3@educa360.pe","password":"Alumno1234","nombres":"Alma","apellidos":"Luna",
                 "dni":"30000003","rol":"ESTUDIANTE","fechaNacimiento":"2011-03-03"}""");

        assertEquals(401, mockMvc.perform(get(RUTA)).andReturn().getResponse().getStatus());
        assertEquals(403, mockMvc.perform(get(RUTA).header("Authorization", "Bearer " + tokenEstudiante))
                .andReturn().getResponse().getStatus());
        assertEquals(200, mockMvc.perform(get(RUTA).header("Authorization", "Bearer " + admin))
                .andReturn().getResponse().getStatus());
    }

    // ==================== Consultas ====================

    @Test
    @DisplayName("El listado filtra por rol")
    void listarFiltraPorRol() throws Exception {
        assertEquals(201, crear(docenteBody("docf@educa360.pe", "50000001"), admin).getResponse().getStatus());
        assertEquals(201, crear(estudianteBody("aluf@educa360.pe", "50000002"), admin).getResponse().getStatus());

        MvcResult docentes = mockMvc.perform(get(RUTA + "?rol=DOCENTE")
                        .header("Authorization", "Bearer " + admin))
                .andReturn();
        MvcResult todos = mockMvc.perform(get(RUTA).header("Authorization", "Bearer " + admin))
                .andReturn();

        String cuerpoDocentes = docentes.getResponse().getContentAsString();
        assertTrue(cuerpoDocentes.contains("docf@educa360.pe"));
        assertTrue(!cuerpoDocentes.contains("aluf@educa360.pe"), "el filtro debe excluir a los estudiantes");
        assertTrue(todos.getResponse().getContentAsString().contains("aluf@educa360.pe"));
    }

    @Test
    @DisplayName("Usuario inexistente: 404")
    void obtenerInexistenteResponde404() throws Exception {
        MvcResult resultado = mockMvc.perform(get(RUTA + "/999")
                        .header("Authorization", "Bearer " + admin))
                .andReturn();

        assertEquals(404, resultado.getResponse().getStatus());
    }

    // ==================== Modificaciones ====================

    @Test
    @DisplayName("La baja lógica corta el acceso al instante")
    void darDeBajaImpideElLogin() throws Exception {
        String body = """
                {"email":"baja@educa360.pe","password":"Baja12345","nombres":"Berta","apellidos":"Gil",
                 "dni":"60000001","rol":"SECRETARIA","telefonos":["911222333"]}""";
        long id = idDe(crear(body, admin));

        assertEquals(200, mockMvc.perform(patch(RUTA + "/" + id + "/activo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"activo\":false}")
                        .header("Authorization", "Bearer " + admin))
                .andReturn().getResponse().getStatus());

        assertEquals(403, login("baja@educa360.pe", "Baja12345"), "dada de baja no debe poder entrar");
        assertTrue(userRepository.findByEmail("baja@educa360.pe").map(u -> !u.isActivo()).orElse(false));
    }

    @Test
    @DisplayName("El ADMIN puede resetear la contraseña y el usuario entra con la nueva")
    void restablecerPassword() throws Exception {
        String body = """
                {"email":"clave@educa360.pe","password":"Original123","nombres":"Carla","apellidos":"Vega",
                 "dni":"70000001","rol":"DOCENTE"}""";
        long id = idDe(crear(body, admin));

        assertEquals(200, login("clave@educa360.pe", "Original123")); // la contraseña inicial sí sirve
        mockMvc.perform(patch(RUTA + "/" + id + "/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"NuevaClave123\"}")
                        .header("Authorization", "Bearer " + admin))
                .andReturn();
        assertEquals(401, login("clave@educa360.pe", "Original123"), "la vieja ya no sirve tras el reset");
        assertEquals(200, login("clave@educa360.pe", "NuevaClave123"));
    }

    @Test
    @DisplayName("Actualizar reemplaza los teléfonos (lista vacía = ninguno)")
    void actualizarReemplazaTelefonos() throws Exception {
        long id = idDe(crear(docenteBody("edit@educa360.pe", "80000001"), admin));

        String cuerpo = actualizar(id, """
                {"nombres":"Dana Editada","apellidos":"Perez","telefonos":["987111222","987333444"]}""");
        assertTrue(cuerpo.contains("987111222") && cuerpo.contains("987333444"));
        assertTrue(cuerpo.contains("Dana Editada"));

        String vacio = actualizar(id, """
                {"nombres":"Dana Editada","apellidos":"Perez","telefonos":[]}""");
        assertTrue(vacio.contains("\"telefonos\":[]"), "lista vacía debe quitar todos");
    }

    // ==================== Helpers ====================

    private MvcResult crear(String body, String token) throws Exception {
        return mockMvc.perform(post(RUTA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .header("Authorization", "Bearer " + token))
                .andReturn();
    }

    private String actualizar(Long id, String body) throws Exception {
        MvcResult resultado = mockMvc.perform(put(RUTA + "/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .header("Authorization", "Bearer " + admin))
                .andReturn();
        assertEquals(200, resultado.getResponse().getStatus(),
                "edición inesperada: " + resultado.getResponse().getContentAsString());
        return resultado.getResponse().getContentAsString();
    }

    private String registrarAdmin() throws Exception {
        MvcResult resultado = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"admin@educa360.pe","password":"Admin12345","nombres":"Ana",
                                 "apellidos":"Ruiz","dni":"10000001","rol":"ADMIN"}"""))
                .andReturn();
        assertEquals(201, resultado.getResponse().getStatus());
        return tokenDe(resultado.getResponse().getContentAsString());
    }

    /** Crea un usuario (como ADMIN) y devuelve su token, para probar RBAC. */
    private String crearYTokenear(String body) throws Exception {
        MvcResult creado = crear(body, admin);
        assertEquals(201, creado.getResponse().getStatus(),
                "no se pudo crear: " + creado.getResponse().getContentAsString());
        Matcher matcher = Pattern.compile("\"email\":\"([^\"]+)\"")
                .matcher(creado.getResponse().getContentAsString());
        assertTrue(matcher.find());
        return loginYToken(matcher.group(1), passwordDe(body));
    }

    private String docenteBody(String email, String dni) {
        return """
                {"email":"%s","password":"Docente1234","nombres":"Dana","apellidos":"Perez",
                 "dni":"%s","rol":"DOCENTE","titulo":"Lic. en Matematicas","telefonos":["987654321"]}
                """.formatted(email, dni);
    }

    private String estudianteBody(String email, String dni) {
        return """
                {"email":"%s","password":"Alumno1234","nombres":"Alma","apellidos":"Luna",
                 "dni":"%s","rol":"ESTUDIANTE","fechaNacimiento":"2010-05-12"}
                """.formatted(email, dni);
    }

    private String passwordDe(String body) {
        Matcher matcher = Pattern.compile("\"password\":\"([^\"]+)\"").matcher(body);
        return matcher.find() ? matcher.group(1) : null;
    }

    private int login(String email, String password) throws Exception {
        MvcResult resultado = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password)))
                .andReturn();
        return resultado.getResponse().getStatus();
    }

    /** Login que exige éxito y devuelve el JWT. */
    private String loginYToken(String email, String password) throws Exception {
        MvcResult resultado = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password)))
                .andReturn();
        assertEquals(200, resultado.getResponse().getStatus(),
                "login inesperado: " + resultado.getResponse().getContentAsString());
        return tokenDe(resultado.getResponse().getContentAsString());
    }

    private long idDe(MvcResult resultado) throws Exception {
        assertEquals(201, resultado.getResponse().getStatus(),
                "creación inesperada: " + resultado.getResponse().getContentAsString());
        Matcher matcher = Pattern.compile("\"id\":(\\d+)").matcher(resultado.getResponse().getContentAsString());
        assertTrue(matcher.find(), "la respuesta debe traer el id");
        return Long.parseLong(matcher.group(1));
    }

    private String tokenDe(String cuerpo) {
        Matcher matcher = TOKEN_PATTERN.matcher(cuerpo);
        assertNotNull(matcher.find(), "debe venir el token");
        return matcher.group(1);
    }
}
