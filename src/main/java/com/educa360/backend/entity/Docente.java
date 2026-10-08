package com.educa360.backend.entity;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity // Marca esta clase como entidad JPA (tabla en la base de datos)
@Table(name = "docentes") // Nombre de la tabla en la base de datos
public class Docente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Relación 1:1 con User (un docente tiene un usuario único)
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "user_id", unique = true, nullable = false)
    private User user;

    // Atributos específicos del docente
    private String titulo; // Ej: "Licenciatura en Matemáticas", "Maestría en Educación"

    @ManyToMany
    @JoinTable(
            name = "docente_materia",
            joinColumns = @JoinColumn(name = "docente_id"),
            inverseJoinColumns = @JoinColumn(name = "materia_id")
    )
    private Set<Materia> materias = new HashSet<>(); // Ej: "Matemáticas", "Física", "Química"

    private LocalDate fechaIngreso;

    @Column
    private LocalDateTime updatedAt;

    private boolean activo; // Para dar de baja lógico sin borrar (soft-delete)

    // Constructors

    public Docente() {
        this.fechaIngreso = LocalDate.now();
        this.activo = true;
    }

    public Docente(User user, String titulo) {
        this();
        this.user = user;
        this.titulo = titulo;
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

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public Set<Materia> getMaterias() {
        return materias;
    }

    public LocalDate getFechaIngreso() {
        return fechaIngreso;
    }

    public void setFechaIngreso(LocalDate fechaIngreso) {
        this.fechaIngreso = fechaIngreso;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    // Métodos para la inserción de materias

    public void agregarMateria(Materia materia) {
        materias.add(materia);
        materia.getDocentes().add(this);
    }

    public void quitarMateria(Materia materia) {
        materias.remove(materia);
        materia.getDocentes().remove(this);
    }

    @PreUpdate
    void update() {
        this.updatedAt = LocalDateTime.now();
    }

    // toString para depuración

    @Override
    public String toString() {
        return "Docente{" +
                "id=" + id +
                ", user=" + user +
                ", titulo='" + titulo + '\'' +
                ", materias=" + materias +
                ", fechaIngreso=" + fechaIngreso +
                ", activo=" + activo +
                '}';
    }

}