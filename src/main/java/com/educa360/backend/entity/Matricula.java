package com.educa360.backend.entity;

import jakarta.persistence.*;

/**
 * Matrícula: el estudiante inscrito en una sección de un período.
 *
 * Es el eslabón que da sección al alumno — sin él, "su curso", su horario y
 * sus hijos no tienen a qué colgarse. Gestionada por SECRETARIA (§3).
 *
 * REGLAS (en MatriculaService):
 * - única por (estudiante, sección): unique en la tabla;
 * - un estudiante NO puede tener dos matrículas en el MISMO período
 *   (ir de 3°A a 3°B es un cambio de sección, no una segunda inscripción);
 * - sólo estudiantes habilitados (Estudiante.estado = ACTIVO).
 *
 * Estados: nace PENDIENTE y SECRETARIA la pasa a PAGADO (§3: es su acción,
 * no una consecuencia automática).
 */
@Entity
@Table(name = "matriculas",
        uniqueConstraints = @UniqueConstraint(columnNames = {"estudiante_id", "seccion_id"}))
public class Matricula extends Auditable {

    public enum Estado {
        PENDIENTE,
        PAGADO
    }

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "estudiante_id", nullable = false)
    private Estudiante estudiante;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "seccion_id", nullable = false)
    private Seccion seccion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Estado estado;

    // Constructors

    public Matricula() {
        this.estado = Estado.PENDIENTE; // nace pendiente: PAGADO es acción de SECRETARIA
    }

    public Matricula(Estudiante estudiante, Seccion seccion) {
        this();
        this.estudiante = estudiante;
        this.seccion = seccion;
    }

    // Getters and Setters

    public Estudiante getEstudiante() {
        return estudiante;
    }

    public void setEstudiante(Estudiante estudiante) {
        this.estudiante = estudiante;
    }

    public Seccion getSeccion() {
        return seccion;
    }

    public void setSeccion(Seccion seccion) {
        this.seccion = seccion;
    }

    public Estado getEstado() {
        return estado;
    }

    public void setEstado(Estado estado) {
        this.estado = estado;
    }

    // toString seguro: sólo ids

    @Override
    public String toString() {
        return "Matricula{" +
                "id=" + getId() +
                ", estudianteId=" + (estudiante != null ? estudiante.getId() : null) +
                ", seccionId=" + (seccion != null ? seccion.getId() : null) +
                ", estado=" + estado +
                '}';
    }
}
