package com.educa360.backend.service;

import com.educa360.backend.dto.AsignacionRequest;
import com.educa360.backend.dto.AsignacionResponse;
import com.educa360.backend.entity.AsignacionDocente;
import com.educa360.backend.entity.Docente;
import com.educa360.backend.entity.Materia;
import com.educa360.backend.entity.Seccion;
import com.educa360.backend.exception.ConflictException;
import com.educa360.backend.exception.NotFoundException;
import com.educa360.backend.repository.AsignacionDocenteRepository;
import com.educa360.backend.repository.DocenteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Asignaciones docentes = "el curso" del colegio (Matemática · 3°A · profe X).
 *
 * Reglas (§5, línea 94):
 * - única por (materia, sección): nadie más da Matemáticas en 3°A → 409;
 * - el docente debe tener la materia habilitada (Docente.materias) → 409;
 * - la materia debe estar activa (activa=false frena nuevas asignaciones) → 409.
 */
@Service
public class AsignacionDocenteService {

    private final AsignacionDocenteRepository asignacionRepository;
    private final DocenteRepository docenteRepository;
    private final MateriaService materiaService;
    private final SeccionService seccionService;

    public AsignacionDocenteService(AsignacionDocenteRepository asignacionRepository,
                                    DocenteRepository docenteRepository,
                                    MateriaService materiaService,
                                    SeccionService seccionService) {
        this.asignacionRepository = asignacionRepository;
        this.docenteRepository = docenteRepository;
        this.materiaService = materiaService;
        this.seccionService = seccionService;
    }

    @Transactional
    public AsignacionResponse crear(AsignacionRequest request) {
        Docente docente = docenteRepository.findById(request.docenteId())
                .orElseThrow(() -> new NotFoundException("El docente " + request.docenteId() + " no existe"));
        Materia materia = materiaService.buscar(request.materiaId());
        Seccion seccion = seccionService.buscar(request.seccionId());

        if (!materia.isActiva()) {
            throw new ConflictException("La materia '" + materia.getNombre()
                    + "' está desactivada: reactívala antes de asignarla");
        }
        if (!docenteRepository.existsByIdAndMaterias_Id(docente.getId(), materia.getId())) {
            throw new ConflictException("La materia '" + materia.getNombre()
                    + "' no está habilitada para ese docente: habilítala antes en /admin/docentes/{id}/materias");
        }
        if (asignacionRepository.existsByMateriaAndSeccion(materia, seccion)) {
            throw new ConflictException("La materia '" + materia.getNombre()
                    + "' ya tiene docente en la sección " + seccion.getEtiqueta());
        }
        return AsignacionResponse.desde(
                asignacionRepository.save(new AsignacionDocente(docente, materia, seccion)));
    }

    /** Filtrable por sección (?seccionId=) o por docente (?docenteId=). */
    @Transactional(readOnly = true)
    public List<AsignacionResponse> listar(Long seccionId, Long docenteId) {
        List<AsignacionDocente> asignaciones;
        if (seccionId != null) {
            asignaciones = asignacionRepository.findBySeccionId(seccionId);
        } else if (docenteId != null) {
            asignaciones = asignacionRepository.findByDocenteId(docenteId);
        } else {
            asignaciones = asignacionRepository.findAll();
        }
        return asignaciones.stream().map(AsignacionResponse::desde).toList();
    }

    /**
     * Quitar la asignación. Es una baja física a propósito: no es histórico de
     * personas (no se desactiva como User), sino configuración del período —
     * las notas de esa asignación (futuro módulo) decidirán si exigen 404.
     */
    @Transactional
    public void eliminar(Long id) {
        AsignacionDocente asignacion = asignacionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("La asignación " + id + " no existe"));
        asignacionRepository.delete(asignacion);
    }
}
