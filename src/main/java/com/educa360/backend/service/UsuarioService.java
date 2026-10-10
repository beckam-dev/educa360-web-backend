package com.educa360.backend.service;

import com.educa360.backend.dto.UsuarioCreateRequest;
import com.educa360.backend.dto.UsuarioResponse;
import com.educa360.backend.dto.UsuarioUpdateRequest;
import com.educa360.backend.entity.Apoderado;
import com.educa360.backend.entity.Docente;
import com.educa360.backend.entity.Estudiante;
import com.educa360.backend.entity.Secretaria;
import com.educa360.backend.entity.Telefono;
import com.educa360.backend.entity.User;
import com.educa360.backend.exception.BadRequestException;
import com.educa360.backend.exception.ConflictException;
import com.educa360.backend.exception.NotFoundException;
import com.educa360.backend.repository.ApoderadoRepository;
import com.educa360.backend.repository.DocenteRepository;
import com.educa360.backend.repository.EstudianteRepository;
import com.educa360.backend.repository.SecretariaRepository;
import com.educa360.backend.repository.UserRepository;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Gestión de usuarios y perfiles (módulo de ADMIN).
 *
 * REGLA CENTRAL: el rol decide el perfil. Al crear una cuenta se genera
 * automáticamente su perfil 1:1 (DOCENTE -> Docente, ESTUDIANTE -> Estudiante...),
 * por lo que es imposible tener un User con un rol que no corresponda a su
 * perfil. Por lo mismo, el rol no es editable: para cambiarlo se da de baja
 * la cuenta y se crea otra.
 */
@Service
public class UsuarioService {

    private final UserRepository userRepository;
    private final DocenteRepository docenteRepository;
    private final EstudianteRepository estudianteRepository;
    private final SecretariaRepository secretariaRepository;
    private final ApoderadoRepository apoderadoRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(
            UserRepository userRepository,
            DocenteRepository docenteRepository,
            EstudianteRepository estudianteRepository,
            SecretariaRepository secretariaRepository,
            ApoderadoRepository apoderadoRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.docenteRepository = docenteRepository;
        this.estudianteRepository = estudianteRepository;
        this.secretariaRepository = secretariaRepository;
        this.apoderadoRepository = apoderadoRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // ==================== Altas ====================

    @Transactional
    public UsuarioResponse crear(UsuarioCreateRequest request) {
        validarReglasDelRol(request);

        if (userRepository.existsByEmail(request.email())) {
            throw new ConflictException("El email " + request.email() + " ya está registrado");
        }
        if (userRepository.existsByDni(request.dni())) {
            throw new ConflictException("El DNI " + request.dni() + " ya está registrado");
        }

        User user = new User(
                request.email(),
                passwordEncoder.encode(request.password()),
                request.nombres(),
                request.apellidos(),
                request.dni(),
                request.rol()
        );
        if (request.telefonos() != null) {
            request.telefonos().forEach(numero -> user.agregarTelefono(new Telefono(numero)));
        }

        User guardado = userRepository.save(user);
        return guardarPerfil(guardado, request);
    }

    /** El rol exige datos propios y prohíbe otros: aquí se hacen cumplir. */
    private void validarReglasDelRol(UsuarioCreateRequest request) {
        if (request.rol() != User.Role.ESTUDIANTE) {
            return;
        }
        if (request.fechaNacimiento() == null) {
            throw new BadRequestException("Un estudiante debe registrar su fecha de nacimiento");
        }
        if (request.telefonos() != null && !request.telefonos().isEmpty()) {
            throw new BadRequestException(
                    "Un estudiante no registra teléfonos propios: la comunicación es con su apoderado");
        }
    }

    /** Crea el perfil 1:1 que corresponde al rol. ADMIN no tiene perfil aparte. */
    private UsuarioResponse guardarPerfil(User user, UsuarioCreateRequest request) {
        return switch (user.getRol()) {
            case DOCENTE -> {
                Docente docente = docenteRepository.save(new Docente(user, request.titulo()));
                yield UsuarioResponse.desde(user, docente.getTitulo(), null, null);
            }
            case ESTUDIANTE -> {
                Estudiante estudiante =
                        estudianteRepository.save(new Estudiante(user, request.fechaNacimiento()));
                yield UsuarioResponse.desde(user, null,
                        estudiante.getFechaNacimiento(), estudiante.getEstado().name());
            }
            case SECRETARIA -> {
                secretariaRepository.save(new Secretaria(user));
                yield UsuarioResponse.desde(user, null, null, null);
            }
            case APODERADO -> {
                apoderadoRepository.save(new Apoderado(user));
                yield UsuarioResponse.desde(user, null, null, null);
            }
            // El administrador no tiene perfil propio: su rol vive en User
            case ADMIN -> UsuarioResponse.desde(user, null, null, null);
        };
    }

    // ==================== Consultas ====================

    /**
     * Listado con filtros combinables (todos opcionales).
     * La búsqueda 'q' cubre email, nombres, apellidos y DNI.
     */
    @Transactional(readOnly = true)
    public List<UsuarioResponse> listar(User.Role rol, Boolean activo, String q) {
        List<Specification<User>> filtros = new ArrayList<>();

        if (rol != null) {
            filtros.add((root, query, cb) -> cb.equal(root.get("rol"), rol));
        }
        if (activo != null) {
            filtros.add((root, query, cb) -> cb.equal(root.get("activo"), activo));
        }
        if (q != null && !q.isBlank()) {
            String patron = "%" + q.trim().toLowerCase() + "%";
            filtros.add((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("email")), patron),
                    cb.like(cb.lower(root.get("nombres")), patron),
                    cb.like(cb.lower(root.get("apellidos")), patron),
                    cb.like(root.get("dni"), patron)));
        }

        Specification<User> spec = filtros.isEmpty()
                ? null
                : filtros.stream().reduce(Specification::and).orElse(null);

        List<User> usuarios = (spec == null)
                ? userRepository.findAll(Sort.by("id"))
                : userRepository.findAll(spec, Sort.by("id"));

        return usuarios.stream().map(this::respuesta).toList();
    }

    @Transactional(readOnly = true)
    public UsuarioResponse obtener(Long id) {
        return respuesta(buscar(id));
    }

    // ==================== Modificaciones ====================

    /**
     * Edita los datos básicos, los teléfonos y los campos del perfil.
     * Teléfonos: null = no tocarlos, lista vacía = quitar todos.
     */
    @Transactional
    public UsuarioResponse actualizar(Long id, UsuarioUpdateRequest request) {
        User user = buscar(id);

        user.setNombres(request.nombres());
        user.setApellidos(request.apellidos());

        if (request.telefonos() != null) {
            user.getTelefonos().clear(); // orphanRemoval borra los que ya no están
            request.telefonos().forEach(numero -> user.agregarTelefono(new Telefono(numero)));
        }

        if (user.getRol() == User.Role.DOCENTE && request.titulo() != null) {
            docenteRepository.findByUser(user).ifPresent(docente -> docente.setTitulo(request.titulo()));
        }
        if (user.getRol() == User.Role.ESTUDIANTE && request.fechaNacimiento() != null) {
            estudianteRepository.findByUser(user)
                    .ifPresent(estudiante -> estudiante.setFechaNacimiento(request.fechaNacimiento()));
        }

        User guardado = userRepository.save(user);
        return respuesta(guardado);
    }

    /** Baja lógica: la cuenta deja de poder entrar; sus datos permanecen. */
    @Transactional
    public UsuarioResponse cambiarActivo(Long id, boolean activo) {
        User user = buscar(id);
        user.setActivo(activo);
        return respuesta(userRepository.save(user));
    }

    /**
     * Reseteo de contraseña por ADMIN.
     * OJO: esto NO invalida los tokens ya emitidos (API stateless); siguen
     * siendo válidos hasta expirar. Cortar el acceso al instante es la baja.
     */
    @Transactional
    public UsuarioResponse restablecerPassword(Long id, String passwordNueva) {
        User user = buscar(id);
        user.setPassword(passwordEncoder.encode(passwordNueva));
        return respuesta(userRepository.save(user));
    }

    // ==================== Helpers ====================

    private User buscar(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("El usuario " + id + " no existe"));
    }

    /** Monta la respuesta leyendo el perfil que corresponda al rol. */
    private UsuarioResponse respuesta(User user) {
        return switch (user.getRol()) {
            case DOCENTE -> docenteRepository.findByUser(user)
                    .map(docente -> UsuarioResponse.desde(user, docente.getTitulo(), null, null))
                    .orElseGet(() -> UsuarioResponse.desde(user, null, null, null));
            case ESTUDIANTE -> estudianteRepository.findByUser(user)
                    .map(estudiante -> UsuarioResponse.desde(user, null,
                            estudiante.getFechaNacimiento(), estudiante.getEstado().name()))
                    .orElseGet(() -> UsuarioResponse.desde(user, null, null, null));
            case SECRETARIA, APODERADO, ADMIN ->
                    UsuarioResponse.desde(user, null, null, null);
        };
    }
}
