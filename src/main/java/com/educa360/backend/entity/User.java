package com.educa360.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String email; // Único, se usará para el login

    private String password; // Contraseña hasheada (BCrypt)

    private String nombres;
    
    private String apellidos;

    @Column(unique = true)
    private String dni; // Documento de identidad

    // Enum de roles del sistema (ADMIN, DOCENTE, ESTUDIANTE, APODERADO)
    public enum Role {
        ADMIN,
        SECRETARIA,
        DOCENTE,
        ESTUDIANTE,
        APODERADO
    }

    @Enumerated(EnumType.STRING)
    private Role rol;

    private boolean activo; // Para dar de baja lógico sin borrar (soft-delete)

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = true)
    private LocalDateTime updatedAt;

    // Constructors

    public User() {
        this.createdAt = LocalDateTime.now();
    }

    public User(String email, String password, String nombres, String apellidos, String dni, Role rol) {
        this();
        this.email = email;
        this.password = password;
        this.nombres = nombres;
        this.apellidos = apellidos;
        this.dni = dni;
        this.rol = rol;
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getNombres() {
        return nombres;
    }

    public void setNombres(String nombres) {
        this.nombres = nombres;
    }

    public String getApellidos() {
        return apellidos;
    }

    public void setApellidos(String apellidos) {
        this.apellidos = apellidos;
    }

    public String getDni() {
        return dni;
    }

    public void setDni(String dni) {
        this.dni = dni;
    }

    public Role getRol() {
        return rol;
    }

    public void setRol(Role rol) {
        this.rol = rol;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    // toString para depuración

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", email='" + email + '\'' +
                ", nombres='" + nombres + '\'' +
                ", apellidos='" + apellidos + '\'' +
                ", dni='" + dni + '\'' +
                ", rol=" + rol +
                ", activo=" + activo +
                ", createdAt=" + createdAt +
                '}';
    }

    @PreUpdate
    void update() {
        this.updatedAt = LocalDateTime.now();
    }

}