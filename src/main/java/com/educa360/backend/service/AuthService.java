package com.educa360.backend.service;

import com.educa360.backend.dto.AuthResponse;
import com.educa360.backend.dto.LoginRequest;
import com.educa360.backend.dto.UsuarioBasicoRequest;
import com.educa360.backend.dto.UserResponse;
import com.educa360.backend.entity.User;
import com.educa360.backend.exception.RateLimitedException;
import com.educa360.backend.repository.UserRepository;
import com.educa360.backend.security.JwtService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final LoginAttemptService loginAttempts;
    private final UsuarioService usuarioService;

    public AuthService(
            UserRepository userRepository,
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            LoginAttemptService loginAttempts,
            UsuarioService usuarioService
    ) {
        this.userRepository = userRepository;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.loginAttempts = loginAttempts;
        this.usuarioService = usuarioService;
    }

    /**
     * Login: valida credenciales (y que la cuenta esté activa) y firma un JWT.
     * Spring Security lanza BadCredentialsException / DisabledException,
     * que GlobalExceptionHandler traduce a 401 / 403.
     *
     * El límite de intentos (5 fallos en 15 min -> 15 min de bloqueo) se
     * comprueba ANTES de validar la contraseña: si no, el atacante seguiría
     * consumiendo bcrypt y CPU durante el bloqueo.
     *
     * @param ip IP del cliente; forma parte de la clave del límite de intentos
     *           para no permitir que alguien bloquee a la víctima a propósito.
     */
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request, String ip) {
        if (loginAttempts.estaBloqueado(request.email(), ip)) {
            throw new RateLimitedException(loginAttempts.restante(request.email(), ip));
        }

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password()));
        } catch (AuthenticationException fallo) {
            loginAttempts.registrarFallo(request.email(), ip);
            throw fallo;
        }

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));

        loginAttempts.registrarExito(request.email(), ip);
        return buildAuthResponse(user);
    }

    /**
     * Arranque del sistema (patrón "bootstrapping").
     *
     * Único caso en que alguien sin cuenta puede crear una: si la BD no tiene
     * NINGÚN usuario, cualquiera crea el primero, y nace siempre como ADMIN
     * (si naciera con otro rol, el sistema quedaría bloqueado: a partir de ahí
     * sólo un ADMIN podría crear cuentas). El "truco" neutraliza además la
     * condición de carrera de count(): como peor caso se crearían dos ADMIN,
     * nunca un sistema sin administradores.
     *
     * En cuanto existe una sola cuenta, este endpoint se cierra (403):
     * de ahí en adelante todas las altas las hace un ADMIN en
     * /api/v1/admin/{usuarios,docentes,estudiantes,secretarias,apoderados}.
     * Un solo interruptor + una sola zona de gestión = sin caminos paralelos.
     *
     * Toda la creación pasa por UsuarioService.crearCuenta(): un solo camino
     * de alta significa una sola verdad (unicidad de email/DNI, hash de
     * contraseña, teléfonos) y ninguna cuenta a medias.
     */
    @Transactional
    public AuthResponse register(UsuarioBasicoRequest request) {
        if (userRepository.count() != 0) {
            throw new AccessDeniedException(
                    "El sistema ya tiene usuarios: las altas las realiza un ADMIN en /api/v1/admin/usuarios");
        }

        User guardado = usuarioService.crearCuenta(request, User.Role.ADMIN);
        return buildAuthResponse(guardado);
    }

    /** Perfil del usuario contenido en el token. */
    @Transactional(readOnly = true)
    public UserResponse me(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));
        return UserResponse.from(user);
    }

    private AuthResponse buildAuthResponse(User user) {
        String token = jwtService.generateToken(user);
        return AuthResponse.bearer(token, jwtService.getExpirationSeconds(), UserResponse.from(user));
    }
}
