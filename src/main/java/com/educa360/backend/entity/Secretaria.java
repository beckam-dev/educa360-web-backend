package com.educa360.backend.entity;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table
public class Secretaria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "user_id", unique = true, nullable = false)
    private User user;

    @Column
    private LocalDateTime updatedAt;

    private boolean activo;

    // CONSTRUCTORS

    public Secretaria() {
        this.activo = true;
    }

    public Secretaria(User user) {
        this();
        this.user = user;
    }

    // GETTERS & SETTERS

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

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    @PreUpdate
    void update() {
        this.updatedAt = LocalDateTime.now();
    }

    // toString para depuración

    @Override
    public String toString() {
        return "Secretaria{" +
                "id=" + id +
                ", user=" + user +
                ", fecha de registro=" + this.user.getCreatedAt() +
                ", activo=" + activo +
                '}';
    }

}
