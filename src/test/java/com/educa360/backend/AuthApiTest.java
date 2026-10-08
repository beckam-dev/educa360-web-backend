package com.educa360.backend;

import com.educa360.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * Tests de integración de la API de autenticación + RBAC.
 *
 * Usa una PostgreSQL efímera (Testcontainers): no toca la BD de desarrollo
 * y se destruye al terminar. Si no hay Docker, los tests se saltan en lugar
 * de fallar.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@AutoConfigureMockMvc
class AuthApiTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    private static final Pattern TOKEN_PATTERN = Pattern.compile("\"token\":\"([^\"]+)\"");
    private static final Pattern MESSAGE_PATTERN = Pattern.compile("\"message\":\"([^\"]*)\"");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void empezarDeCero() {
        userRepository.deleteAll(); // cada test ve una BD vacía (alta inicial posible)
    }

    // ==================== Autenticación ====================

    @Test
    @DisplayName("GET /me sin token responde 401")
    void sinTokenResponde401() throws Exception {
        assertEquals(401, mockMvc.perform(get("/api/v1/auth/me")).andReturn().getResponse().getStatus());
    }

    @Test
    @DisplayName("El primer usuario se crea SIEMPRE como ADMIN aunque pida otro rol")
    void altaInicialFuerzaRolAdmin() throws Exception {
        MvcResult resultado = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("primer@educa360.pe", "Secreta123", "11112222", "DOCENTE")))
                .andReturn();

        assertEquals(201, resultado.getResponse().getStatus());
        assertTrue(resultado.getResponse().getContentAsString().contains("\"rol\":\"ADMIN\""),
                "El rol del request debe ignorarse en el alta inicial");
    }

    @Test
    @DisplayName("Después del alta inicial, el registro anónimo se cierra (403)")
    void registroAnonimoSeCierraTrasElPrimerUsuario() throws Exception {
        String admin = registrar("admin@educa360.pe", "Admin12345", "12345678", "ADMIN", null);

        assertEquals(403, enviarPost("/api/v1/auth/register",
                cuerpo("intruso@x.pe", "Intruso123", "99998888", "ADMIN"), null).getResponse().getStatus());
        assertNotNull(admin);
    }

    @Test
    @DisplayName("Login correcto devuelve token y el perfil, SIN la contraseña")
    void loginCorrectoDevuelveTokenSinPassword() throws Exception {
        registrar("admin@educa360.pe", "Admin12345", "12345678", "ADMIN", null);

        MvcResult login = enviarPost("/api/v1/auth/login",
                loginBody("admin@educa360.pe", "Admin12345"), null);
        String cuerpo = login.getResponse().getContentAsString();

        assertEquals(200, login.getResponse().getStatus());
        assertNotNull(tokenDe(cuerpo), "Debe venir el JWT");
        assertTrue(cuerpo.contains("\"tokenType\":\"Bearer\""));
        assertTrue(!cuerpo.contains("password"), "La contraseña jamás debe salir en la respuesta");

        // y con ese token, /me devuelve el perfil
        MvcResult me = mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + tokenDe(cuerpo)))
                .andReturn();
        assertEquals(200, me.getResponse().getStatus());
        assertTrue(me.getResponse().getContentAsString().contains("admin@educa360.pe"));
        assertTrue(!me.getResponse().getContentAsString().contains("password"));
    }

    @Test
    @DisplayName("Contraseña incorrecta o email inexistente: 401 con el mismo mensaje")
    void credencialesInvalidasResponde401() throws Exception {
        registrar("admin@educa360.pe", "Admin12345", "12345678", "ADMIN", null);

        MvcResult mala = enviarPost("/api/v1/auth/login",
                loginBody("admin@educa360.pe", "clave-mala"), null);
        MvcResult inexistente = enviarPost("/api/v1/auth/login",
                loginBody("nadie@x.pe", "Loquesea123"), null);

        assertEquals(401, mala.getResponse().getStatus());
        assertEquals(401, inexistente.getResponse().getStatus());
        // Sin enumeración de usuarios: mismo mensaje en ambos casos
        assertNotNull(mensajeDe(mala));
        assertEquals(mensajeDe(mala), mensajeDe(inexistente));
    }

    @Test
    @DisplayName("El email no distingue mayúsculas ni espacios")
    void emailSeNormaliza() throws Exception {
        registrar("  Lucila@Educa360.pe ", "Lucila1234", "22223333", null, null);

        MvcResult login = enviarPost("/api/v1/auth/login",
                loginBody("LUCILA@educa360.pe", "Lucila1234"), null);

        assertEquals(200, login.getResponse().getStatus());
        assertEquals(1, userRepository.count());
    }

    @Test
    @DisplayName("DNI sin 8 dígitos se rechaza con 400")
    void dniInvalidoResponde400() throws Exception {
        MvcResult resultado = enviarPost("/api/v1/auth/register",
                cuerpo("dni@x.pe", "Password123", "12A45", "ADMIN"), null);

        assertEquals(400, resultado.getResponse().getStatus());
        assertTrue(resultado.getResponse().getContentAsString().contains("8 dígitos"));
    }

    @Test
    @DisplayName("Email ya registrado responde 409")
    void emailDuplicadoResponde409() throws Exception {
        String token = registrar("admin@educa360.pe", "Admin12345", "12345678", "ADMIN", null);

        MvcResult duplicado = enviarPost("/api/v1/auth/register",
                cuerpo("admin@educa360.pe", "OtraClave123", "87654321", "DOCENTE"), token);

        assertEquals(409, duplicado.getResponse().getStatus());
        assertTrue(duplicado.getResponse().getContentAsString().contains("ya está registrado"));
    }

    @Test
    @DisplayName("Rol con un valor que no existe responde 400, no 500")
    void rolInvalidoResponde400() throws Exception {
        String token = registrar("admin@educa360.pe", "Admin12345", "12345678", "ADMIN", null);

        MvcResult resultado = enviarPost("/api/v1/auth/register",
                "{\"email\":\"x@y.pe\",\"password\":\"Prueba1234\",\"nombres\":\"A\"," +
                        "\"apellidos\":\"B\",\"dni\":\"12345678\",\"rol\":\"FOO\"}", token);

        assertEquals(400, resultado.getResponse().getStatus());
    }

    @Test
    @DisplayName("JSON malformado responde 400, no 500")
    void jsonMalformadoResponde400() throws Exception {
        MvcResult resultado = enviarPost("/api/v1/auth/login", "{esto no es json", null);

        assertEquals(400, resultado.getResponse().getStatus());
    }

    @Test
    @DisplayName("Ruta inexistente responde 404 y método no permitido 405")
    void erroresDeRouting() throws Exception {
        String token = registrar("admin@educa360.pe", "Admin12345", "12345678", "ADMIN", null);

        MvcResult noExiste = mockMvc.perform(get("/api/v1/noexiste")
                        .header("Authorization", "Bearer " + token))
                .andReturn();
        MvcResult metodoNoPermitido = mockMvc.perform(put("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andReturn();

        assertEquals(404, noExiste.getResponse().getStatus());
        assertEquals(405, metodoNoPermitido.getResponse().getStatus());
    }

    @Test
    @DisplayName("Cuenta dada de baja no puede iniciar sesión (403)")
    void cuentaDesactivadaNoPuedeLoguear() throws Exception {
        registrar("baja@educa360.pe", "Baja12345", "33334444", "ESTUDIANTE", null);
        userRepository.findByEmail("baja@educa360.pe").ifPresent(usuario -> {
            usuario.setActivo(false);
            userRepository.save(usuario);
        });

        MvcResult login = enviarPost("/api/v1/auth/login",
                loginBody("baja@educa360.pe", "Baja12345"), null);

        assertEquals(403, login.getResponse().getStatus());
        assertTrue(login.getResponse().getContentAsString().contains("inactivo"));
    }

    // ==================== RBAC ====================

    @Test
    @DisplayName("Sólo el rol correcto entra en cada zona")
    void rbacPorRol() throws Exception {
        String tokenAdmin = registrar("admin@educa360.pe", "Admin12345", "12345678", "ADMIN", null);
        String tokenEstudiante = registrar("lucia@educa360.pe", "Lucia12345", "87654321", "ESTUDIANTE", tokenAdmin);

        assertEquals(200, getConToken("/api/v1/demo/admin", tokenAdmin));
        assertEquals(403, getConToken("/api/v1/demo/admin", tokenEstudiante));
        assertEquals(200, getConToken("/api/v1/demo/estudiante", tokenEstudiante));
        assertEquals(403, getConToken("/api/v1/demo/secretaria", tokenAdmin));
        assertEquals(200, getConToken("/api/v1/demo/authenticated", tokenEstudiante));
        assertEquals(401, getConToken("/api/v1/demo/authenticated", null));
    }

    // ==================== Helpers ====================

    private MvcResult enviarPost(String url, String body, String bearer) throws Exception {
        var peticion = post(url).contentType(MediaType.APPLICATION_JSON).content(body);
        if (bearer != null) {
            peticion = peticion.header("Authorization", "Bearer " + bearer);
        }
        return mockMvc.perform(peticion).andReturn();
    }

    private int getConToken(String url, String bearer) throws Exception {
        var peticion = get(url);
        if (bearer != null) {
            peticion = peticion.header("Authorization", "Bearer " + bearer);
        }
        return mockMvc.perform(peticion).andReturn().getResponse().getStatus();
    }

    /** Registra una cuenta y devuelve su token (o null si se pide sin bearer). */
    private String registrar(String email, String password, String dni, String rol, String bearer) throws Exception {
        MvcResult resultado = enviarPost("/api/v1/auth/register",
                cuerpo(email, password, dni, rol), bearer);
        assertEquals(201, resultado.getResponse().getStatus(),
                "Registro inesperado: " + resultado.getResponse().getContentAsString());
        return tokenDe(resultado.getResponse().getContentAsString());
    }

    private String cuerpo(String email, String password, String dni, String rol) {
        String rolJson = (rol == null) ? "null" : "\"" + rol + "\"";
        return ("{\"email\":\"%s\",\"password\":\"%s\",\"nombres\":\"Prueba\"," +
                "\"apellidos\":\"Testeo\",\"dni\":\"%s\",\"rol\":%s}")
                .formatted(email, password, dni, rolJson);
    }

    private String loginBody(String email, String password) {
        return "{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password);
    }

    private String tokenDe(String cuerpo) {
        Matcher matcher = TOKEN_PATTERN.matcher(cuerpo);
        return matcher.find() ? matcher.group(1) : null;
    }

    private String mensajeDe(MvcResult resultado) throws Exception {
        Matcher matcher = MESSAGE_PATTERN.matcher(resultado.getResponse().getContentAsString());
        return matcher.find() ? matcher.group(1) : null;
    }
}
