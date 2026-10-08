package com.educa360.backend.entity;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "estudiantes")
public class Estudiante extends Auditable {

    // Relación 1:1 con User (un estudiante tiene un usuario único).
    // Sin orphanRemoval: la baja lógica se controla con User.activo.
    @OneToOne(optional = false, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "user_id", unique = true, nullable = false)
    private User user;

    // Datos propios del estudiante
    @Column(nullable = false)
    private LocalDate fechaNacimiento;

    public enum EstadoEstudiante {
        ACTIVO,
        TRASLADADO,
        RETIRADO,
        EGRESADO
    }

    // Estado académico (semántica distinta de User.activo, que es la baja de la cuenta)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoEstudiante estado;

    // Constructors

    public Estudiante() {
        this.estado = EstadoEstudiante.ACTIVO;
    }

    public Estudiante(User user, LocalDate fechaNacimiento) {
        this();
        this.user = user;
        this.fechaNacimiento = fechaNacimiento;
    }

    // Getters and Setters

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public LocalDate getFechaNacimiento() {
        return fechaNacimiento;
    }

    public void setFechaNacimiento(LocalDate fechaNacimiento) {
        this.fechaNacimiento = fechaNacimiento;
    }

    public EstadoEstudiante getEstado() {
        return estado;
    }

    public void setEstado(EstadoEstudiante estado) {
        this.estado = estado;
    }

    /*
     * Un estudiante está habilitado institucionalmente
     * mientras se encuentre en estado ACTIVO.
     *
     * Esto NO significa que esté matriculado en el periodo actual.
     * La matrícula se manejará mediante la entidad Matricula.
     */
    public boolean estaHabilitado() {
        return this.estado == EstadoEstudiante.ACTIVO;
    }

    // toString seguro: sólo ids y campos simples (sin colecciones lazy)

    @Override
    public String toString() {
        return "Estudiante{" +
                "id=" + getId() +
                ", userId=" + (user != null ? user.getId() : null) +
                ", fechaNacimiento=" + fechaNacimiento +
                ", estado=" + estado +
                '}';
    }
}
