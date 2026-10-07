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

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "user_id", unique = true, nullable = false)
    private User user;

    private LocalDate fechaNacimiento;

    private LocalDate fechaIngreso;

    public enum EstadoEstudiante {
        ACTIVO,
        TRASLADADO,
        RETIRADO,
        EGRESADO
    }

    @Enumerated(EnumType.STRING)
    private EstadoEstudiante estado = EstadoEstudiante.ACTIVO;

    @Column(nullable = true)
    private LocalDateTime updatedAt;

    // Constructors

    public Estudiante() {
        this.fechaIngreso = LocalDate.now();
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

    public LocalDate getFechaIngreso() {
        return fechaIngreso;
    }

    public void setFechaIngreso(LocalDate fechaIngreso) {
        this.fechaIngreso = fechaIngreso;
    }

    public EstadoEstudiante getEstado() {
        return estado;
    }

    public void setEstado(EstadoEstudiante estado) {
        this.estado = estado;
    }

    // Método helper: ¿El estudiante, esta matrículado?

    public boolean estaHabilitado() {
        return this.estado == EstadoEstudiante.ACTIVO;
    }

    // toString para depuración

    @Override
    public String toString() {
        return "Estudiante{" +
                "id=" + id +
                ", user=" + user +
                ", fechaNacimiento=" + fechaNacimiento +
                ", fechaIngreso=" + fechaIngreso +
                ", estado=" + estado +
                ", updatedAt=" + (updatedAt == null ? "No ha sufrido cambios" : updatedAt) +
                '}';
    }

    @PreUpdate
    void update() {
        this.updatedAt = LocalDateTime.now();
    }

}
