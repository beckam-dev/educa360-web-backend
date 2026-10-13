package com.educa360.backend.entity;

import jakarta.persistence.*;

import java.time.LocalDate;

/**
 * Período académico (bimestre) de la institución. Ej.: "Bimestre I, 2026".
 *
 * REGLA CENTRAL: sólo puede haber UNO ABIERTO a la vez — es "el período
 * vigente" al que apuntarán asistencia y calificaciones (§5, regla del docente).
 *
 * Nace CERRADO y se abre explícitamente con PATCH /admin/periodos/{id}/estado:
 * así la regla "uno a la vez" vive en un solo método, y el hecho de abrir un
 * período queda como una acción deliberada, no como un efecto lateral del alta.
 */
@Entity
@Table(name = "periodos_academicos")
public class PeriodoAcademico extends Auditable {

    public enum Estado {
        ABIERTO,
        CERRADO
    }

    @Column(unique = true, nullable = false)
    private String nombre; // Ej.: "Bimestre I, 2026"

    @Column(nullable = false)
    private LocalDate fechaInicio;

    @Column(nullable = false)
    private LocalDate fechaFin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Estado estado;

    // Constructors

    public PeriodoAcademico() {
        this.estado = Estado.CERRADO; // nace cerrado: se abre a propósito
    }

    public PeriodoAcademico(String nombre, LocalDate fechaInicio, LocalDate fechaFin) {
        this();
        this.nombre = nombre;
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
    }

    // Getters and Setters

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public LocalDate getFechaInicio() {
        return fechaInicio;
    }

    public void setFechaInicio(LocalDate fechaInicio) {
        this.fechaInicio = fechaInicio;
    }

    public LocalDate getFechaFin() {
        return fechaFin;
    }

    public void setFechaFin(LocalDate fechaFin) {
        this.fechaFin = fechaFin;
    }

    public Estado getEstado() {
        return estado;
    }

    public void setEstado(Estado estado) {
        this.estado = estado;
    }

    @Override
    public String toString() {
        return "PeriodoAcademico{" +
                "id=" + getId() +
                ", nombre='" + nombre + '\'' +
                ", fechaInicio=" + fechaInicio +
                ", fechaFin=" + fechaFin +
                ", estado=" + estado +
                '}';
    }
}
