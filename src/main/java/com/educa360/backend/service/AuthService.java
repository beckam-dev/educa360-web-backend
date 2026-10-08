package com.educa360.backend.service;

import com.educa360.backend.dto.AuthResponse;
import com.educa360.backend.dto.LoginRequest;
import com.educa360.backend.dto.RegisterRequest;
import com.educa360.backend.dto.UserResponse;
import com.educa360.backend.entity.User;
import com.educa360.backend.exception.ConflictException;
import com.educa360.backend.repository.UserRepository;
import com.educa360.backend.security.JwtService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final String ROLE_ADMIN = "ROLE_ADMIN";

    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            UserRepository userRepository,
            AuthenticationManager authenticationManager,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.authenticationManager = authenticationManager;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    /**
     * Login: valida credenciales (y que la cuenta esté activa) y firma un JWT.
     * Spring Security lanza BadCredentialsException / DisabledException,
     * que GlobalExceptionHandler traduce a 401 / 403.
     */
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));

        return buildAuthResponse(user);
    }

    /**
     * Alta de cuentas.
     *
     * Bootstrap: si la BD no tiene NINGÚN usuario, cualquiera puede crear
     * el primero. En ese caso el rol SIEMPRE es ADMIN (se ignora el del
     * request): si el primer usuario naciera con otro rol, el sistema
     * quedaría bloqueado, porque a partir de ahí sólo un ADMIN puede
     * crear cuentas. Ese mismo "truco" neutraliza la condición de carrera
     * de count(): como peor caso se crearían dos ADMIN, nunca un sistema
     * sin administradores.
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        boolean bootstrap = userRepository.count() == 0;

        if (!bootstrap && !isCurrentAdmin()) {
            throw new AccessDeniedException("Sólo un ADMIN puede crear cuentas de usuario");
        }

        if (userRepository.existsByEmail(request.email())) {
            throw new ConflictException("El email " + request.email() + " ya está registrado");
        }

        User.Role rol = bootstrap
                ? User.Role.ADMIN  // forzado: no se confía en el request
                : (request.rol() != null ? request.rol() : User.Role.ADMIN);

        User user = new User(
                request.email(),
                passwordEncoder.encode(request.password()),
                request.nombres(),
                request.apellidos(),
                request.dni(),
                rol
        );

        User guardado = userRepository.save(user);
        return buildAuthResponse(guardado);
    }

    /** Perfil del usuario contenido en el token. */
    @Transactional(readOnly = true)
    public UserResponse me(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));
        return UserResponse.from(user);
    }

    private boolean isCurrentAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> ROLE_ADMIN.equals(authority.getAuthority()));
    }

    private AuthResponse buildAuthResponse(User user) {
        String token = jwtService.generateToken(user);
        return AuthResponse.bearer(token, jwtService.getExpirationSeconds(), UserResponse.from(user));
    }
}
