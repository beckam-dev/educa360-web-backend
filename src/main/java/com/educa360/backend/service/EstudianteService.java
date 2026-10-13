package com.educa360.backend.service;

import com.educa360.backend.dto.EstudianteRequest;
import com.educa360.backend.dto.UsuarioBasicoRequest;
import com.educa360.backend.dto.UsuarioResponse;
import com.educa360.backend.entity.Estudiante;
import com.educa360.backend.entity.User;
import com.educa360.backend.exception.BadRequestException;
import com.educa360.backend.repository.EstudianteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Alta y reglas del perfil ESTUDIANTE.
 *
 * Reglas que gobierna este servicio (no el de usuarios):
 * - su ficha exige fecha de nacimiento (@NotNull en EstudianteRequest);
 * - no registra teléfonos propios: al ser menores de edad, la comunicación
 *   es siempre con su apoderado.
 */
@Service
public class EstudianteService {

    private final UsuarioService usuarioService;
    private final EstudianteRepository estudianteRepository;

    public EstudianteService(UsuarioService usuarioService, EstudianteRepository estudianteRepository) {
        this.usuarioService = usuarioService;
        this.estudianteRepository = estudianteRepository;
    }

    /** Crea la cuenta y su perfil Estudiante en la misma transacción. */
    @Transactional
    public UsuarioResponse crear(UsuarioBasicoRequest datos, EstudianteRequest perfil) {
        if (datos.telefonos() != null && !datos.telefonos().isEmpty()) {
            throw new BadRequestException(
                    "Un estudiante no registra teléfonos propios: la comunicación es con su apoderado");
        }

        User user = usuarioService.crearCuenta(datos, User.Role.ESTUDIANTE);
        estudianteRepository.save(new Estudiante(user, perfil.fechaNacimiento()));
        return usuarioService.aRespuesta(user);
    }
}
