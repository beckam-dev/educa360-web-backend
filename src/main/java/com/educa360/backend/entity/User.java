package com.educa360.backend.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
public class User extends Auditable {

    // Único y obligatorio: es la credencial de login
    @Column(unique = true, nullable = false)
    @NotBlank
    @Email
    private String email;

    // Contraseña hasheada (BCrypt).
    // WRITE_ONLY: Jackson nunca la incluye en una respuesta JSON.
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String nombres;

    @Column(nullable = false)
    private String apellidos;

    @Column(unique = true, nullable = false)
    private String dni; // Documento de identidad

    // Enum de roles del sistema (ADMIN, SECRETARIA, DOCENTE, ESTUDIANTE, APODERADO)
    public enum Role {
        ADMIN,
        SECRETARIA,
        DOCENTE,
        ESTUDIANTE,
        APODERADO
    }

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role rol;

    // Única fuente de baja lógica del sistema (soft-delete).
    // Los perfiles (Docente, Secretaria, ...) no duplican este estado.
    @Column(nullable = false)
    private boolean activo = true;

    /*
     * Teléfonos de contacto del usuario.
     *
     * Vive aquí porque todos los perfiles son 1:1 con User:
     * Docente, Secretaria y Apoderado lo usan;
     * Estudiante no (al ser menor, la comunicación es con su apoderado).
     */
    @OneToMany(
            mappedBy = "user",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private Set<Telefono> telefonos = new HashSet<>();

    // Constructors

    public User() {
        this.activo = true;
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

    public Set<Telefono> getTelefonos() {
        return telefonos;
    }

    // Métodos helper: mantienen sincronizados ambos lados de la relación

    public void agregarTelefono(Telefono telefono) {
        telefonos.add(telefono);
        telefono.setUser(this);
    }

    public void quitarTelefono(Telefono telefono) {
        telefonos.remove(telefono);
        telefono.setUser(null);
    }

    // toString para depuración (nunca incluye la contraseña)

    @Override
    public String toString() {
        return "User{" +
                "id=" + getId() +
                ", email='" + email + '\'' +
                ", nombres='" + nombres + '\'' +
                ", apellidos='" + apellidos + '\'' +
                ", dni='" + dni + '\'' +
                ", rol=" + rol +
                ", activo=" + activo +
                ", createdAt=" + getCreatedAt() +
                '}';
    }
}
