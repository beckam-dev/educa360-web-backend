package com.educa360.backend.entity;

import jakarta.persistence.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "apoderados")
public class Apoderado extends Auditable {

    /*
     * Cada apoderado tiene una cuenta de usuario asociada.
     * Sin orphanRemoval: la baja lógica se controla con User.activo.
     */
    @OneToOne(optional = false, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "user_id", unique = true, nullable = false)
    private User user;

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

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    // Teléfonos: la colección vive en User (User.telefonos); el apoderado sólo delega.

    public Set<Telefono> getTelefonos() {
        return user == null ? Set.of() : user.getTelefonos();
    }

    public void agregarTelefono(Telefono telefono) {
        user.agregarTelefono(telefono);
    }

    public void quitarTelefono(Telefono telefono) {
        user.quitarTelefono(telefono);
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

    @Override
    public String toString() {
        return "Apoderado{" +
                "id=" + getId() +
                ", user=" + user +
                ", telefonos=" + getTelefonos() +
                ", estudiantes=" + estudiantes +
                '}';
    }
}
