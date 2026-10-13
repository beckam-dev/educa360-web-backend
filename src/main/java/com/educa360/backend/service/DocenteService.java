package com.educa360.backend.service;

import com.educa360.backend.dto.DocenteMateriasRequest;
import com.educa360.backend.dto.DocenteRequest;
import com.educa360.backend.dto.MateriaResponse;
import com.educa360.backend.dto.UsuarioBasicoRequest;
import com.educa360.backend.dto.UsuarioResponse;
import com.educa360.backend.entity.Docente;
import com.educa360.backend.entity.Materia;
import com.educa360.backend.entity.User;
import com.educa360.backend.exception.NotFoundException;
import com.educa360.backend.repository.DocenteRepository;
import com.educa360.backend.repository.MateriaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Dueño del perfil DOCENTE y de sus habilitaciones de materia
 * (Docente.materias → join table docente_materia).
 *
 * La habilitación contesta "qué materias PUEDE enseñar"; cuáles enseña
 * realmente en un período lo decide {@link CursoService},
 * que exige la habilitación como prerrequisito.
 */
@Service
public class DocenteService {

    private final UsuarioService usuarioService;
    private final DocenteRepository docenteRepository;
    private final MateriaRepository materiaRepository;

    public DocenteService(UsuarioService usuarioService,
                          DocenteRepository docenteRepository,
                          MateriaRepository materiaRepository) {
        this.usuarioService = usuarioService;
        this.docenteRepository = docenteRepository;
        this.materiaRepository = materiaRepository;
    }

    /** Crea la cuenta y su perfil Docente en la misma transacción. */
    @Transactional
    public UsuarioResponse crear(UsuarioBasicoRequest datos, DocenteRequest perfil) {
        User user = usuarioService.crearCuenta(datos, User.Role.DOCENTE);
        docenteRepository.save(new Docente(user, perfil.titulo()));
        return usuarioService.aRespuesta(user);
    }

    /** Materias habilitadas del docente (ficha del docente en el panel). */
    @Transactional(readOnly = true)
    public List<MateriaResponse> listarMaterias(Long docenteId) {
        return buscar(docenteId).getMaterias().stream().map(MateriaResponse::desde).toList();
    }

    /**
     * Reemplaza el conjunto de habilitaciones: el PUT pinta la hoja entera
     * (como el teléfono del módulo de usuarios) — no suma ni resta filas,
     * de modo que el resultado no depende del estado previo.
     */
    @Transactional
    public List<MateriaResponse> habilitarMaterias(Long docenteId, DocenteMateriasRequest request) {
        Docente docente = buscar(docenteId);
        List<Materia> materias = request.materiaIds().stream()
                .distinct()
                .map(id -> materiaRepository.findById(id)
                        .orElseThrow(() -> new NotFoundException("La materia " + id + " no existe")))
                .toList();
        // La join table la gobierna el lado Docente: clear + agregarMateria la reescribe completa.
        docente.getMaterias().clear();
        materias.forEach(docente::agregarMateria);
        return docenteRepository.save(docente).getMaterias().stream().map(MateriaResponse::desde).toList();
    }

    private Docente buscar(Long id) {
        return docenteRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("El docente " + id + " no existe"));
    }
}
