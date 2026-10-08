package com.educa360.backend.security;

import com.educa360.backend.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.function.Function;

/**
 * Emisión y validación de JWT (HS256).
 *
 * El token es stateless: contiene el email como subject y el rol como claim,
 * por lo que no hace falta consultar la BD en cada petición (salvo para
 * comprobar que la cuenta siga activa y exista).
 */
@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    /** HS256 exige una clave de al menos 32 bytes. */
    private static final int MIN_SECRET_BYTES = 32;

    /** Prefijo del secreto de desarrollo: si se detecta, se avisa por log. */
    private static final String DEV_SECRET_MARKER = "educa360-dev-";

    private final SecretKey signingKey;
    private final long expirationMs;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-ms:86400000}") long expirationMs
    ) {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "app.jwt.secret debe tener al menos 32 bytes para firmar con HS256");
        }
        if (secret.startsWith(DEV_SECRET_MARKER)) {
            log.warn("Se está usando el secreto JWT de DESARROLLO. " +
                    "Define APP_JWT_SECRET con una clave propia antes de publicar.");
        }
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    // Emisión

    public String generateToken(User user) {
        Date now = new Date();
        return Jwts.builder()
                .subject(user.getEmail())
                .claim("rol", user.getRol().name())
                .claim("nombre", user.getNombres() + " " + user.getApellidos())
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationMs))
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
    }

    // Lectura

    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public <T> T extractClaim(String token, Function<Claims, T> resolver) {
        Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return resolver.apply(claims);
    }

    // Validación

    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            return extractEmail(token).equals(userDetails.getUsername())
                    && !isExpired(token);
        } catch (JwtException | IllegalArgumentException e) {
            // Firma inválida, token malformado o expirado
            return false;
        }
    }

    private boolean isExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }

    public long getExpirationSeconds() {
        return expirationMs / 1000;
    }
}
