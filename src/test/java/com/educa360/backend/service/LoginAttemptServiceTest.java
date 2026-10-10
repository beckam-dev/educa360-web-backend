package com.educa360.backend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests unitarios del límite de intentos: sin Spring, sin BD, sin esperas reales.
 * El tiempo se manipula con un reloj falso.
 */
class LoginAttemptServiceTest {

    private static final String EMAIL = "victima@educa360.pe";
    private static final String IP = "10.0.0.7";
    private static final Duration QUINCE_MIN = Duration.ofMinutes(15);

    private RelojDePrueba reloj;
    private LoginAttemptService servicio;

    @BeforeEach
    void setUp() {
        reloj = new RelojDePrueba();
        servicio = new LoginAttemptService(5, QUINCE_MIN, QUINCE_MIN, reloj);
    }

    @Test
    @DisplayName("4 fallos no bloquean; el 5.º sí (límite exacto)")
    void limiteExactoDeIntentos() {
        for (int i = 1; i <= 4; i++) {
            servicio.registrarFallo(EMAIL, IP);
            assertFalse(servicio.estaBloqueado(EMAIL, IP), "no debe bloquear con " + i + " fallos");
        }

        servicio.registrarFallo(EMAIL, IP); // 5.º

        assertTrue(servicio.estaBloqueado(EMAIL, IP));
        assertTrue(servicio.restante(EMAIL, IP).isPositive());
    }

    @Test
    @DisplayName("El cooldown dura exactamente 15 minutos desde el último fallo")
    void cooldownDeQuinceMinutos() {
        for (int i = 0; i < 5; i++) {
            servicio.registrarFallo(EMAIL, IP);
        }

        reloj.avanzar(Duration.ofMinutes(14));
        assertTrue(servicio.estaBloqueado(EMAIL, IP));
        assertEquals(Duration.ofMinutes(1), servicio.restante(EMAIL, IP));

        reloj.avanzar(Duration.ofMinutes(1)); // 15 min en total
        assertFalse(servicio.estaBloqueado(EMAIL, IP));
        assertEquals(Duration.ZERO, servicio.restante(EMAIL, IP));
    }

    @Test
    @DisplayName("Un acierto reinicia el contador")
    void aciertoReiniciaElContador() {
        for (int i = 0; i < 3; i++) {
            servicio.registrarFallo(EMAIL, IP);
        }
        servicio.registrarExito(EMAIL, IP); // entra quien sabe la clave

        for (int i = 0; i < 4; i++) {
            servicio.registrarFallo(EMAIL, IP);
        }
        assertFalse(servicio.estaBloqueado(EMAIL, IP), "el acierto debió limpiar los fallos anteriores");
    }

    @Test
    @DisplayName("La ventana reinicia el conteo si pasan 15 minutos sin fallos")
    void ventanaDeConteoExpira() {
        for (int i = 0; i < 4; i++) {
            servicio.registrarFallo(EMAIL, IP);
        }

        reloj.avanzar(Duration.ofMinutes(16));
        servicio.registrarFallo(EMAIL, IP); // 5.º fallo, pero el anterior ya caducó

        assertFalse(servicio.estaBloqueado(EMAIL, IP), "el contador debe haber empezado de nuevo");
    }

    @Test
    @DisplayName("La clave es email+IP: no se bloquea a otros ni desde otro equipo")
    void claveCompuestaEmailMasIp() {
        for (int i = 0; i < 5; i++) {
            servicio.registrarFallo(EMAIL, IP);
        }

        assertTrue(servicio.estaBloqueado(EMAIL, IP), "el que falla queda bloqueado");
        assertFalse(servicio.estaBloqueado("otra@persona.pe", IP), "otro usuario no debe verse afectado");
        assertFalse(servicio.estaBloqueado(EMAIL, "10.0.0.99"), "la víctima desde otro equipo no queda bloqueada");
    }

    @Test
    @DisplayName("Sin fallos no hay bloqueo ni tiempo de espera")
    void sinFallosNoBloquea() {
        assertFalse(servicio.estaBloqueado(EMAIL, IP));
        assertEquals(Duration.ZERO, servicio.restante(EMAIL, IP));
    }

    /** Reloj manipulable: el test decide cuánto tiempo pasa. */
    private static final class RelojDePrueba extends Clock {

        private Instant ahora = Instant.parse("2026-10-09T10:00:00Z");

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return ahora;
        }

        void avanzar(Duration duracion) {
            ahora = ahora.plus(duracion);
        }
    }
}
