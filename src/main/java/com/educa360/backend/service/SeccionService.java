package com.educa360.backend.service;

import com.educa360.backend.dto.SeccionRequest;
import com.educa360.backend.dto.SeccionResponse;
import com.educa360.backend.entity.Docente;
import com.educa360.backend.entity.PeriodoAcademico;
import com.educa360.backend.entity.Seccion;
import com.educa360.backend.exception.ConflictException;
import com.educa360.backend.exception.NotFoundException;
import com.educa360.backend.repository.DocenteRepository;
import com.educa360.backend.repository.SeccionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Secciones: los grupos de alumnos por período ("3°A").
 *
 * Sólo secundaria → el DTO acota grado 1..5, sin campo "nivel".
 * Regla: "grado+letra" es único DENTRO de un período (3°A del Bimestre I y
 * 3°A del Bimestre II son filas distintas — las secciones se re-forman cada período).
 */
@Service
public class SeccionService {

    private final SeccionRepository seccionRepository;
    private final PeriodoAcademicoService periodoService;
    private final DocenteRepository docenteRepository;

    public SeccionService(SeccionRepository seccionRepository,
                          PeriodoAcademicoService periodoService,
                          DocenteRepository docenteRepository) {
        this.seccionRepository = seccionRepository;
        this.periodoService = periodoService;
        this.docenteRepository = docenteRepository;
    }

    @Transactional
    public SeccionResponse crear(SeccionRequest request) {
        PeriodoAcademico periodo = periodoService.buscar(request.periodoId());
        Docente tutor = buscarTutor(request.tutorId());
        if (seccionRepository.existsByGradoAndLetraAndPeriodo(request.grado(), request.letra(), periodo)) {
            throw new ConflictException("Ya existe la sección " + request.grado() + "°" + request.letra()
                    + " en el período " + periodo.getNombre());
        }
        Seccion seccion = new Seccion(request.grado(), request.letra(), request.aula(), tutor, periodo);
        return SeccionResponse.desde(seccionRepository.save(seccion));
    }

    /** Listado completo, o filtrado por período (?periodoId=) para el panel. */
    @Transactional(readOnly = true)
    public List<SeccionResponse> listar(Long periodoId) {
        List<Seccion> secciones = periodoId == null
                ? seccionRepository.findAll()
                : seccionRepository.findByPeriodoId(periodoId);
        return secciones.stream().map(SeccionResponse::desde).toList();
    }

    /**
     * Edición del grupo: grado, letra, aula y tutor. NO mueve la sección de
     * período — cambiarla de período es crear otra fila (se re-forma cada período),
     * arrastrarla rompería el histórico de asistencia y notas por período.
     */
    @Transactional
    public SeccionResponse actualizar(Long id, SeccionRequest request) {
        Seccion seccion = buscar(id);
        PeriodoAcademico periodo = seccion.getPeriodo();
        if (!periodo.getId().equals(request.periodoId())) {
            throw new ConflictException("La sección no se puede mover de período: crea una nueva en "
                    + request.periodoId());
        }
        Docente tutor = buscarTutor(request.tutorId());
        boolean cambioDeNombre = !seccion.getGrado().equals(request.grado())
                || !seccion.getLetra().equals(request.letra());
        if (cambioDeNombre
                && seccionRepository.existsByGradoAndLetraAndPeriodo(request.grado(), request.letra(), periodo)) {
            throw new ConflictException("Ya existe la sección " + request.grado() + "°" + request.letra()
                    + " en el período " + periodo.getNombre());
        }
        seccion.setGrado(request.grado());
        seccion.setLetra(request.letra());
        seccion.setAula(request.aula());
        seccion.setTutor(tutor);
        return SeccionResponse.desde(seccionRepository.save(seccion));
    }

    // Helpers

    @Transactional(readOnly = true)
    public Seccion buscar(Long id) {
        return seccionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("La sección " + id + " no existe"));
    }

    /** El tutor es opcional; si viene, debe existir y ser un perfil Docente. */
    private Docente buscarTutor(Long tutorId) {
        if (tutorId == null) {
            return null;
        }
        return docenteRepository.findById(tutorId)
                .orElseThrow(() -> new NotFoundException("El docente tutor " + tutorId + " no existe"));
    }
}
