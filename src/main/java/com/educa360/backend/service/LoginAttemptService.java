package com.educa360.backend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Anti fuerza bruta en el login.
 *
 * Regla: 5 intentos fallidos dentro de 15 minutos bloquean el login
 * durante otros 15 minutos (medidos desde el último fallo). Un acierto
 * reinicia el contador. Mientras está bloqueado, el endpoint responde
 * 429 con el header Retry-After antes de validar la contraseña.
 *
 * La clave es email + IP:
 * - Por email sólo: un atacante podría bloquear a la víctima a propósito (DoS).
 * - Por IP sólo: en una red de instituto (NAT) un solo equipo bloquearía a todos.
 * Con la clave compuesta, el que queda limitado es el que está fallando.
 *
 * El estado vive en memoria: se pierde al reiniciar, suficiente con una
 * sola instancia. Si algún día hay varias instancias o se quiere persistencia,
 * haría falta Redis/BD (fuera del alcance actual).
 *
 * NOTA sobre la IP: se usa {@code request.getRemoteAddr()}, que es la IP real
 * salvo que haya un proxy/nginx delante. En ese caso activar
 * {@code server.forward-headers-strategy=framework} para que Spring lea el
 * X-Forwarded-For por nosotros; NO leer el header a mano, porque cualquier
 * cliente externo podría falsificarlo y eludir el bloqueo.
 */
@Component
public class LoginAttemptService {

    private final int maxIntentos;
    private final Duration ventana;
    private final Duration cooldown;
    private final Clock clock;
    private final Map<String, Registro> registros = new ConcurrentHashMap<>();

    @Autowired
    public LoginAttemptService(
            @Value("${app.security.login.max-attempts:5}") int maxIntentos,
            @Value("${app.security.login.window-minutes:15}") long ventanaMinutos,
            @Value("${app.security.login.cooldown-minutes:15}") long cooldownMinutos
    ) {
        this(maxIntentos, Duration.ofMinutes(ventanaMinutos),
                Duration.ofMinutes(cooldownMinutos), Clock.systemUTC());
    }

    /** Visible para tests: inyecta un reloj falso y duraciones cortas. */
    LoginAttemptService(int maxIntentos, Duration ventana, Duration cooldown, Clock clock) {
        this.maxIntentos = maxIntentos;
        this.ventana = ventana;
        this.cooldown = cooldown;
        this.clock = clock;
    }

    /** ¿Se superó el máximo de fallos y aún no pasó el cooldown? */
    public boolean estaBloqueado(String email, String ip) {
        String clave = clave(email, ip);
        Registro registro = registros.get(clave);
        if (registro == null) {
            return false;
        }
        Instant ahora = clock.instant();

        if (registro.fallos() < maxIntentos) {
            // Fuera de la ventana de conteo el registro ya no sirve
            if (expirado(registro.ultimoFallo(), ahora, ventana)) {
                registros.remove(clave);
            }
            return false;
        }
        return !expirado(registro.ultimoFallo(), ahora, cooldown);
    }

    /** Cuánto falta para poder volver a intentar (ZERO si no está bloqueado). */
    public Duration restante(String email, String ip) {
        Registro registro = registros.get(clave(email, ip));
        if (registro == null || registro.fallos() < maxIntentos) {
            return Duration.ZERO;
        }
        Duration falta = Duration.between(clock.instant(), registro.ultimoFallo().plus(cooldown));
        return falta.isNegative() ? Duration.ZERO : falta;
    }

    public void registrarFallo(String email, String ip) {
        String clave = clave(email, ip);
        Instant ahora = clock.instant();
        registros.compute(clave, (k, r) ->
                (r == null || expirado(r.ultimoFallo(), ahora, ventana))
                        ? new Registro(1, ahora)                     // empieza de nuevo
                        : new Registro(r.fallos() + 1, ahora));
    }

    public void registrarExito(String email, String ip) {
        registros.remove(clave(email, ip));
    }

    /** Reinicia todos los contadores (uso en tests). */
    public void limpiar() {
        registros.clear();
    }

    private String clave(String email, String ip) {
        return email + "|" + ip;
    }

    /** ¿pasó ya "duracion" desde "instante"? */
    private boolean expirado(Instant instante, Instant ahora, Duration duracion) {
        return !instante.plus(duracion).isAfter(ahora);
    }

    private record Registro(int fallos, Instant ultimoFallo) {
    }
}
