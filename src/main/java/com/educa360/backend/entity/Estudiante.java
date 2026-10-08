package com.educa360.backend.entity;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "estudiantes")
public class Estudiante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /*
     * Cada estudiante tiene una cuenta de usuario asociada.
     */
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "user_id", unique = true, nullable = false)
    private User user;

    /*
     * Datos propios del estudiante.
     */
    @Column(nullable = false)
    private LocalDate fechaNacimiento;

    public enum EstadoEstudiante {
        ACTIVO,
        TRASLADADO,
        RETIRADO,
        EGRESADO
    }

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoEstudiante estado;

    @Column
    private LocalDateTime updatedAt;

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

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
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

    @PreUpdate
    void update() {
        this.updatedAt = LocalDateTime.now();
    }

    @Override
    public String toString() {
        return "Estudiante{" +
                "id=" + id +
                ", user=" + user +
                ", fechaNacimiento=" + fechaNacimiento +
                ", fecha de registro=" + this.user.getCreatedAt() +
                ", estado=" + estado +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
