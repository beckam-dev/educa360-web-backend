package com.educa360.backend.security;

import com.educa360.backend.entity.User;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests unitarios de JwtService: sin Spring, sin BD, sin red.
 * Son los más rápidos y los que protegen la firma del token.
 */
class JwtServiceTest {

    private static final String SECRETO = "clave-de-pruebas-suficientemente-larga-1234567890";
    private static final String OTRO_SECRETO = "otro-secreto-completamente-distinto-0987654321";
    private static final String EMAIL = "tester@educa360.pe";

    private JwtService jwtService;
    private User usuario;
    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRETO, 3_600_000L); // 1 hora
        usuario = new User(EMAIL, "hash-bcrypt", "Tes", "Tor", "12345678", User.Role.ADMIN);
        userDetails = org.springframework.security.core.userdetails.User.builder()
                .username(EMAIL)
                .password("hash-bcrypt")
                .authorities("ROLE_ADMIN")
                .disabled(false)
                .build();
    }

    @Test
    @DisplayName("El token contiene el email como subject y el rol como claim")
    void generaTokenConSubjectYRol() {
        String token = jwtService.generateToken(usuario);

        assertEquals(EMAIL, jwtService.extractEmail(token));
        assertEquals("ADMIN", jwtService.extractClaim(token, claims -> claims.get("rol", String.class)));
    }

    @Test
    @DisplayName("Un token recién emitido es válido para su dueño")
    void tokenRecienteEsValido() {
        String token = jwtService.generateToken(usuario);

        assertTrue(jwtService.isTokenValid(token, userDetails));
    }

    @Test
    @DisplayName("Un token firmado con OTRO secreto se rechaza")
    void tokenDeOtroSecretoNoEsValido() {
        String token = new JwtService(OTRO_SECRETO, 3_600_000L).generateToken(usuario);

        assertFalse(jwtService.isTokenValid(token, userDetails));
    }

    @Test
    @DisplayName("Un token expirado se rechaza")
    void tokenExpiradoNoEsValido() {
        JwtService caducado = new JwtService(SECRETO, -1_000L); // ya vencido
        String token = caducado.generateToken(usuario);

        assertFalse(caducado.isTokenValid(token, userDetails));
        assertThrows(JwtException.class, () -> caducado.extractEmail(token));
    }

    @Test
    @DisplayName("Un token manipulado (firma alterada) se rechaza")
    void tokenManipuladoNoEsValido() {
        String token = jwtService.generateToken(usuario);
        String manipulado = token.substring(0, token.length() - 4) + "zzzz";

        assertFalse(jwtService.isTokenValid(manipulado, userDetails));
    }

    @Test
    @DisplayName("Un token no puede usarse a nombre de otro usuario")
    void tokenNoSirveParaOtroUsuario() {
        String token = jwtService.generateToken(usuario);
        UserDetails otro = org.springframework.security.core.userdetails.User.builder()
                .username("otra@persona.pe")
                .password("x")
                .authorities("ROLE_ESTUDIANTE")
                .build();

        assertFalse(jwtService.isTokenValid(token, otro));
    }

    @Test
    @DisplayName("Un secreto de menos de 32 bytes no se acepta (HS256)")
    void secretoCortoLanzaErrorDeArranque() {
        assertThrows(IllegalStateException.class, () -> new JwtService("corto", 1_000L));
    }
}
