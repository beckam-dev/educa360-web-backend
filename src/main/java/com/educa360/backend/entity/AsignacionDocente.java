package com.educa360.backend.entity;

import jakarta.persistence.*;

/**
 * Quién enseña qué a quién en un período: en el lenguaje del colegio esto es
 * "el curso" ("Matemática de 3°A, con el profe X").
 *
 * NO existe entidad Curso: la asignación YA ES el curso concreto de un
 * período — la sección trae el periodo dentro, y una entidad intermedia
 * sólo sería un intermediario sin comportamiento propio.
 *
 * Sin horario por ahora: si algún día hace falta asistencia por bloque,
 * el horario se añade aquí (día + hora) con sus reglas de solape.
 *
 * REGLAS (viven en AsignacionDocenteService):
 * - única por (materia, sección): nadie más da Matemáticas en 3°A;
 * - el docente debe tener la materia habilitada (Docente.materias);
 * - la materia debe estar activa.
 */
@Entity
@Table(name = "asignaciones_docentes",
        uniqueConstraints = @UniqueConstraint(columnNames = {"materia_id", "seccion_id"}))
public class AsignacionDocente extends Auditable {

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "docente_id", nullable = false)
    private Docente docente;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "materia_id", nullable = false)
    private Materia materia;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "seccion_id", nullable = false)
    private Seccion seccion;

    // Constructors

    public AsignacionDocente() {
    }

    public AsignacionDocente(Docente docente, Materia materia, Seccion seccion) {
        this();
        this.docente = docente;
        this.materia = materia;
        this.seccion = seccion;
    }

    // Getters and Setters

    public Docente getDocente() {
        return docente;
    }

    public void setDocente(Docente docente) {
        this.docente = docente;
    }

    public Materia getMateria() {
        return materia;
    }

    public void setMateria(Materia materia) {
        this.materia = materia;
    }

    public Seccion getSeccion() {
        return seccion;
    }

    public void setSeccion(Seccion seccion) {
        this.seccion = seccion;
    }

    // toString seguro para logs: sólo ids

    @Override
    public String toString() {
        return "AsignacionDocente{" +
                "id=" + getId() +
                ", docenteId=" + (docente != null ? docente.getId() : null) +
                ", materiaId=" + (materia != null ? materia.getId() : null) +
                ", seccionId=" + (seccion != null ? seccion.getId() : null) +
                '}';
    }
}
