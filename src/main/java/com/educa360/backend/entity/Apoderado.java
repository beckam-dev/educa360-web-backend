package com.educa360.backend.entity;

import jakarta.persistence.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "apoderados")
public class Apoderado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /*
     * Cada apoderado tiene una cuenta de usuario asociada.
     */
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "user_id", unique = true, nullable = false)
    private User user;

    /*
     * Un apoderado puede tener varios números de contacto.
     */
    @OneToMany(
            mappedBy = "apoderado",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private Set<Telefono> telefonos = new HashSet<>();

    /*
     * Relaciones entre este apoderado y sus estudiantes representados.
     *
     * No usamos ManyToMany directamente porque la relación
     * tiene información propia: parentesco, responsable principal, etc.
     */
    @OneToMany(
            mappedBy = "apoderado",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private Set<ApoderadoEstudiante> estudiantes = new HashSet<>();

    // Constructors

    public Apoderado() {
    }

    public Apoderado(User user) {
        this.user = user;
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

    public Set<Telefono> getTelefonos() {
        return telefonos;
    }

    public Set<ApoderadoEstudiante> getEstudiantes() {
        return estudiantes;
    }

    /*
     * Métodos helper para mantener ambos lados de la relación sincronizados.
     */

    public void agregarEstudiante(ApoderadoEstudiante relacion) {
        estudiantes.add(relacion);
        relacion.setApoderado(this);
    }

    public void quitarEstudiante(ApoderadoEstudiante relacion) {
        estudiantes.remove(relacion);
        relacion.setApoderado(null);
    }
}
