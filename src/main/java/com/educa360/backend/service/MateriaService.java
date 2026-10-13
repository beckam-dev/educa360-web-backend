package com.educa360.backend.service;

import com.educa360.backend.dto.MateriaRequest;
import com.educa360.backend.dto.MateriaResponse;
import com.educa360.backend.entity.Materia;
import com.educa360.backend.exception.ConflictException;
import com.educa360.backend.exception.NotFoundException;
import com.educa360.backend.repository.MateriaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Materia = "lo que se enseña" (catálogo institucional: Matemática, Física...).
 * No lleva sección ni docente: eso vive en {@link AsignacionDocenteService}.
 *
 * Desactivar una materia ({@code activa=false}) sólo impide NUEVAS asignaciones;
 * las existentes se conservan — es un botón de "ya no la damos más", no de borrado.
 */
@Service
public class MateriaService {

    private final MateriaRepository materiaRepository;

    public MateriaService(MateriaRepository materiaRepository) {
        this.materiaRepository = materiaRepository;
    }

    @Transactional
    public MateriaResponse crear(MateriaRequest request) {
        if (materiaRepository.existsByNombre(request.nombre())) {
            throw new ConflictException("Ya existe la materia '" + request.nombre() + "' en el catálogo");
        }
        return MateriaResponse.desde(materiaRepository.save(new Materia(request.nombre(), request.descripcion())));
    }

    @Transactional(readOnly = true)
    public List<MateriaResponse> listar() {
        return materiaRepository.findAll().stream().map(MateriaResponse::desde).toList();
    }

    @Transactional
    public MateriaResponse actualizar(Long id, MateriaRequest request) {
        Materia materia = buscar(id);
        if (materiaRepository.existsByNombre(request.nombre()) && !materia.getNombre().equals(request.nombre())) {
            throw new ConflictException("Ya existe la materia '" + request.nombre() + "' en el catálogo");
        }
        materia.setNombre(request.nombre());
        materia.setDescripcion(request.descripcion());
        return MateriaResponse.desde(materiaRepository.save(materia));
    }

    /** Activar / desactivar: alterna el flag que consulta AsignacionDocenteService. */
    @Transactional
    public MateriaResponse cambiarActiva(Long id, boolean activa) {
        Materia materia = buscar(id);
        materia.setActiva(activa);
        return MateriaResponse.desde(materiaRepository.save(materia));
    }

    @Transactional(readOnly = true)
    public Materia buscar(Long id) {
        return materiaRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("La materia " + id + " no existe"));
    }
}
