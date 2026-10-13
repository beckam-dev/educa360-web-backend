package com.educa360.backend.service;

import com.educa360.backend.dto.DocenteRequest;
import com.educa360.backend.dto.UsuarioBasicoRequest;
import com.educa360.backend.dto.UsuarioResponse;
import com.educa360.backend.entity.Docente;
import com.educa360.backend.entity.User;
import com.educa360.backend.repository.DocenteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Alta y reglas del perfil DOCENTE.
 *
 * Hoy sólo crea el perfil; el dueño natural de este dominio crecerá con el
 * catálogo académico (asignar/quitar materias).
 */
@Service
public class DocenteService {

    private final UsuarioService usuarioService;
    private final DocenteRepository docenteRepository;

    public DocenteService(UsuarioService usuarioService, DocenteRepository docenteRepository) {
        this.usuarioService = usuarioService;
        this.docenteRepository = docenteRepository;
    }

    /** Crea la cuenta y su perfil Docente en la misma transacción. */
    @Transactional
    public UsuarioResponse crear(UsuarioBasicoRequest datos, DocenteRequest perfil) {
        User user = usuarioService.crearCuenta(datos, User.Role.DOCENTE);
        docenteRepository.save(new Docente(user, perfil.titulo()));
        return usuarioService.aRespuesta(user);
    }
}
