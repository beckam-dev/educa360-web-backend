package com.educa360.backend.service;

import com.educa360.backend.dto.MatriculaEstadoRequest;
import com.educa360.backend.dto.MatriculaRequest;
import com.educa360.backend.dto.MatriculaResponse;
import com.educa360.backend.entity.Estudiante;
import com.educa360.backend.entity.Matricula;
import com.educa360.backend.entity.PeriodoAcademico;
import com.educa360.backend.entity.Seccion;
import com.educa360.backend.exception.ConflictException;
import com.educa360.backend.exception.NotFoundException;
import com.educa360.backend.repository.EstudianteRepository;
import com.educa360.backend.repository.MatriculaRepository;
import com.educa360.backend.repository.PeriodoAcademicoRepository;
import com.educa360.backend.repository.SeccionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Matrículas: el estudiante queda inscrito en una sección de un período.
 *
 * Reglas que gobierna (Gestión de matrículas = SECRETARIA, §3):
 * - nace PENDIENTE; SÓLO SECRETARIA la pasa a PAGADO (su acción, no automática);
 * - única por (estudiante, sección) — el unique está en la tabla;
 * - un estudiante no puede tener DOS matriculas en el MISMO período:
 *   ir de 3°A a 3°B es un traslado, no una segunda inscripción;
 * - sólo alumnos habilitados (Estudiante.estado = ACTIVO).
 *
 * "Matrícula vigente" = la del período ABIERTO (el vigente, §5): es la que
 * da sección al alumno para /estudiante/cursos, su ficha y los hijos del
 * apoderado. Sin período abierto no hay vigente.
 */
@Service
public class MatriculaService {

    private final MatriculaRepository matriculaRepository;
    private final EstudianteRepository estudianteRepository;
    private final SeccionRepository seccionRepository;
    private final PeriodoAcademicoRepository periodoRepository;

    public MatriculaService(MatriculaRepository matriculaRepository,
                            EstudianteRepository estudianteRepository,
                            SeccionRepository seccionRepository,
                            PeriodoAcademicoRepository periodoRepository) {
        this.matriculaRepository = matriculaRepository;
        this.estudianteRepository = estudianteRepository;
        this.seccionRepository = seccionRepository;
        this.periodoRepository = periodoRepository;
    }

    /** Alta desde Secretaría: 404 si no existe el alumno o la sección, 409 por reglas. */
    @Transactional
    public MatriculaResponse crear(MatriculaRequest datos) {
        Estudiante estudiante = estudianteRepository.findById(datos.estudianteId())
                .orElseThrow(() -> new NotFoundException("El estudiante " + datos.estudianteId() + " no existe"));
        Seccion seccion = seccionRepository.findById(datos.seccionId())
                .orElseThrow(() -> new NotFoundException("La sección " + datos.seccionId() + " no existe"));

        if (!estudiante.estaHabilitado()) {
            throw new ConflictException("El estudiante está " + estudiante.getEstado().name()
                    + ": sólo se matriculan alumnos ACTIVOS");
        }
        if (matriculaRepository.existsByEstudianteAndSeccion(estudiante, seccion)) {
            throw new ConflictException("El estudiante ya está matriculado en esa sección");
        }
        if (matriculaRepository.existsByEstudianteIdAndSeccionPeriodoId(
                estudiante.getId(), seccion.getPeriodo().getId())) {
            throw new ConflictException("El estudiante ya tiene una matrícula en el período "
                    + seccion.getPeriodo().getNombre()
                    + ": cambiar de sección dentro del mismo período es un traslado");
        }

        return MatriculaResponse.desde(matriculaRepository.save(new Matricula(estudiante, seccion)));
    }

    /** Cambio de estado (PENDIENTE -> PAGADO es la acción de Secretaría). */
    @Transactional
    public MatriculaResponse cambiarEstado(Long id, MatriculaEstadoRequest datos) {
        Matricula matricula = matriculaRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("La matrícula " + id + " no existe"));
        matricula.setEstado(datos.estado());
        return MatriculaResponse.desde(matricula);
    }

    /**
     * La matrícula vigente del alumno: la del período ABIERTO.
     * Sin período abierto (o sin matrícula) → vacío.
     */
    @Transactional(readOnly = true)
    public Optional<Matricula> vigenteDe(Long estudianteId) {
        return periodoRepository.findByEstado(PeriodoAcademico.Estado.ABIERTO)
                .flatMap(periodo ->
                        matriculaRepository.findByEstudianteIdAndSeccionPeriodoId(estudianteId, periodo.getId()));
    }
}
