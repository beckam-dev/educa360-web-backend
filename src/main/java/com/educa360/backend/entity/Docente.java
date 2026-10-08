package com.educa360.backend.entity;

import jakarta.persistence.*;

import java.util.HashSet;
import java.util.Set;

@Entity // Marca esta clase como entidad JPA (tabla en la base de datos)
@Table(name = "docentes") // Nombre de la tabla en la base de datos
public class Docente extends Auditable {

    // Relación 1:1 con User (un docente tiene un usuario único).
    // Sin orphanRemoval: desactivar la cuenta NO borra el perfil ni viceversa
    // (la baja lógica se controla con User.activo).
    @OneToOne(optional = false, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "user_id", unique = true, nullable = false)
    private User user;

    // Atributos específicos del docente
    private String titulo; // Ej: "Licenciatura en Matemáticas", "Maestría en Educación"

    // Materias que este docente está habilitado para enseñar (catálogo de la institución)
    @ManyToMany
    @JoinTable(
            name = "docente_materia",
            joinColumns = @JoinColumn(name = "docente_id"),
            inverseJoinColumns = @JoinColumn(name = "materia_id"),
            // El Set evita duplicados en memoria; esto los impide también en la BD
            uniqueConstraints = @UniqueConstraint(columnNames = {"docente_id", "materia_id"})
    )
    private Set<Materia> materias = new HashSet<>(); // Ej: "Matemáticas", "Física", "Química"

    // Constructors

    public Docente() {
    }

    public Docente(User user, String titulo) {
        this();
        this.user = user;
        this.titulo = titulo;
    }

    // Getters and Setters

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

    // Teléfonos: la colección vive en User (User.telefonos); el perfil sólo delega.
    public Set<Telefono> getTelefonos() {
        return user == null ? Set.of() : user.getTelefonos();
    }

    public void agregarTelefono(Telefono telefono) {
        user.agregarTelefono(telefono);
    }

    public void quitarTelefono(Telefono telefono) {
        user.quitarTelefono(telefono);
    }

    // Métodos helper para mantener sincronizados ambos lados de la relación

    public void agregarMateria(Materia materia) {
        materias.add(materia);
        materia.getDocentes().add(this);
    }

    public void quitarMateria(Materia materia) {
        materias.remove(materia);
        materia.getDocentes().remove(this);
    }

    // toString para depuración

    @Override
    public String toString() {
        return "Docente{" +
                "id=" + getId() +
                ", user=" + user +
                ", titulo='" + titulo + '\'' +
                ", materias=" + materias +
                ", fecha de registro=" + (user != null ? user.getCreatedAt() : null) +
                '}';
    }

}
