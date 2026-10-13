package com.educa360.backend.service;

import com.educa360.backend.dto.SecretariaRequest;
import com.educa360.backend.dto.UsuarioBasicoRequest;
import com.educa360.backend.dto.UsuarioResponse;
import com.educa360.backend.entity.Secretaria;
import com.educa360.backend.entity.User;
import com.educa360.backend.repository.SecretariaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Alta y reglas del perfil SECRETARIA.
 *
 * Su campo es la coordinación de matrículas, justificaciones y estados del
 * estudiante; hoy sólo se crea el perfil (SecretariaRequest va vacío), y aquí
 * vivirán esas reglas cuando llegue ese módulo.
 */
@Service
public class SecretariaService {

    private final UsuarioService usuarioService;
    private final SecretariaRepository secretariaRepository;

    public SecretariaService(UsuarioService usuarioService, SecretariaRepository secretariaRepository) {
        this.usuarioService = usuarioService;
        this.secretariaRepository = secretariaRepository;
    }

    /** Crea la cuenta y su perfil Secretaria en la misma transacción. */
    @Transactional
    public UsuarioResponse crear(UsuarioBasicoRequest datos, SecretariaRequest perfil) {
        User user = usuarioService.crearCuenta(datos, User.Role.SECRETARIA);
        secretariaRepository.save(new Secretaria(user));
        return usuarioService.aRespuesta(user);
    }
}
