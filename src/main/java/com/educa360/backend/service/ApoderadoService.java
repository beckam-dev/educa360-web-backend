package com.educa360.backend.service;

import com.educa360.backend.dto.ApoderadoRequest;
import com.educa360.backend.dto.UsuarioBasicoRequest;
import com.educa360.backend.dto.UsuarioResponse;
import com.educa360.backend.entity.Apoderado;
import com.educa360.backend.entity.User;
import com.educa360.backend.repository.ApoderadoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Alta y reglas del perfil APODERADO.
 *
 * Aquí vivirán las reglas de representación (asignar/quitar estudiantes,
 * quién es responsable principal) cuando arranque el módulo de familias.
 */
@Service
public class ApoderadoService {

    private final UsuarioService usuarioService;
    private final ApoderadoRepository apoderadoRepository;

    public ApoderadoService(UsuarioService usuarioService, ApoderadoRepository apoderadoRepository) {
        this.usuarioService = usuarioService;
        this.apoderadoRepository = apoderadoRepository;
    }

    /** Crea la cuenta y su perfil Apoderado en la misma transacción. */
    @Transactional
    public UsuarioResponse crear(UsuarioBasicoRequest datos, ApoderadoRequest perfil) {
        User user = usuarioService.crearCuenta(datos, User.Role.APODERADO);
        apoderadoRepository.save(new Apoderado(user));
        return usuarioService.aRespuesta(user);
    }
}
