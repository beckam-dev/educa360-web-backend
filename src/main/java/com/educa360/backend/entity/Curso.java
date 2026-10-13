package com.educa360.backend.entity;

import jakarta.persistence.*;

/**
 * Curso: la oferta concreta de un período — en el lenguaje del colegio,
 * "el curso" (Matemática de 3°A, con el profe X).
 *
 * Antes llamada AsignacionDocente: el nombre describía la acción de asignar;
 * ahora la clase es la identidad que cuelgan asistencia, notas y materiales,
 * y que además carga su horario (1:N con {@link HorarioBloque}).
 *
 * No es un alias de {@link Materia}: la materia es el catálogo ("lo que se
 * enseña"), el curso es la instancia de un período, en una sección, con docente.
 *
 * REGLAS (en CursoService):
 * - única por (materia, sección): nadie más da Matemáticas en 3°A;
 * - el docente debe tener la materia habilitada (Docente.materias);
 * - la materia debe estar activa.
 */
@Entity
@Table(name = "cursos",
        uniqueConstraints = @UniqueConstraint(columnNames = {"materia_id", "seccion_id"}))
public class Curso extends Auditable {

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

    public Curso() {
    }

    public Curso(Docente docente, Materia materia, Seccion seccion) {
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
        return "Curso{" +
                "id=" + getId() +
                ", docenteId=" + (docente != null ? docente.getId() : null) +
                ", materiaId=" + (materia != null ? materia.getId() : null) +
                ", seccionId=" + (seccion != null ? seccion.getId() : null) +
                '}';
    }
}
