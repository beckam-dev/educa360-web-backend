package com.educa360.backend.service;

import com.educa360.backend.dto.PeriodoRequest;
import com.educa360.backend.dto.PeriodoResponse;
import com.educa360.backend.entity.PeriodoAcademico;
import com.educa360.backend.exception.BadRequestException;
import com.educa360.backend.exception.ConflictException;
import com.educa360.backend.exception.NotFoundException;
import com.educa360.backend.repository.PeriodoAcademicoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Períodos académicos (§5, línea 92).
 *
 * Regla central del módulo: SÓLO UNO ABIERTO A LA VEZ — el abierto es "el
 * período vigente" al que apuntarán asistencia y calificaciones. El período
 * nace CERRADO; abrirlo es una acción explícita (PATCH /estado), lo que deja
 * la regla concentrada en un solo método y trazable.
 */
@Service
public class PeriodoAcademicoService {

    private final PeriodoAcademicoRepository periodoRepository;

    public PeriodoAcademicoService(PeriodoAcademicoRepository periodoRepository) {
        this.periodoRepository = periodoRepository;
    }

    /** Alta: nace CERRADO (ver javadoc). */
    @Transactional
    public PeriodoResponse crear(PeriodoRequest request) {
        validarFechas(request);
        if (periodoRepository.existsByNombre(request.nombre())) {
            throw new ConflictException("Ya existe un período con el nombre '" + request.nombre() + "'");
        }
        PeriodoAcademico periodo = new PeriodoAcademico(request.nombre(), request.fechaInicio(), request.fechaFin());
        return PeriodoResponse.desde(periodoRepository.save(periodo));
    }

    @Transactional(readOnly = true)
    public List<PeriodoResponse> listar() {
        return periodoRepository.findAll().stream().map(PeriodoResponse::desde).toList();
    }

    @Transactional
    public PeriodoResponse actualizar(Long id, PeriodoRequest request) {
        PeriodoAcademico periodo = buscar(id);
        validarFechas(request);
        if (periodoRepository.existsByNombre(request.nombre()) && !periodo.getNombre().equals(request.nombre())) {
            throw new ConflictException("Ya existe un período con el nombre '" + request.nombre() + "'");
        }
        periodo.setNombre(request.nombre());
        periodo.setFechaInicio(request.fechaInicio());
        periodo.setFechaFin(request.fechaFin());
        return PeriodoResponse.desde(periodoRepository.save(periodo));
    }

    /**
     * Abrir / cerrar. Abrir con otro vigente es 409: el modelo admite UN solo
     * ABIERTO, que es el que usan asistencia y calificaciones para acotarse.
     */
    @Transactional
    public PeriodoResponse cambiarEstado(Long id, PeriodoAcademico.Estado estado) {
        PeriodoAcademico periodo = buscar(id);
        if (estado == PeriodoAcademico.Estado.ABIERTO
                && periodoRepository.existsByEstadoAndIdNot(PeriodoAcademico.Estado.ABIERTO, id)) {
            throw new ConflictException("Ya hay un período abierto; cierra el vigente antes de abrir otro "
                    + "(sólo puede haber uno a la vez)");
        }
        periodo.setEstado(estado); // cerrar siempre es válido (el histórico se conserva)
        return PeriodoResponse.desde(periodoRepository.save(periodo));
    }

    // Helpers

    @Transactional(readOnly = true)
    public PeriodoAcademico buscar(Long id) {
        return periodoRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("El período " + id + " no existe"));
    }

    private void validarFechas(PeriodoRequest request) {
        if (request.fechaFin().isBefore(request.fechaInicio())) {
            throw new BadRequestException("La fecha de fin no puede ser anterior a la de inicio");
        }
    }
}
