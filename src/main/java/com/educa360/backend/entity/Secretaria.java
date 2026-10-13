package com.educa360.backend.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "secretarias")
public class Secretaria extends Auditable {

    /*
     * Perfil 1:1 con User.
     * Sin orphanRemoval: la baja lógica se controla con User.activo.
     */
    @OneToOne(optional = false, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "user_id", unique = true, nullable = false)
    private User user;

    // CONSTRUCTORS

    public Secretaria() {
    }

    public Secretaria(User user) {
        this();
        this.user = user;
    }

    // GETTERS & SETTERS

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    // toString seguro: sólo ids y campos simples

    @Override
    public String toString() {
        return "Secretaria{" +
                "id=" + getId() +
                ", userId=" + (user != null ? user.getId() : null) +
                '}';
    }

}
