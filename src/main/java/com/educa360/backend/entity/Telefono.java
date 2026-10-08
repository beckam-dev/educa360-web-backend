package com.educa360.backend.entity;

import jakarta.persistence.*;

import java.util.Objects;

@Entity
@Table(
        name = "telefonos",
        // La misma persona no debe repetir el mismo número.
        // Refuerza en BD lo que el Set ya garantiza en memoria.
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "numero"})
)
public class Telefono extends Auditable {

    @Column(nullable = false)
    private String numero;

    /*
     * Dueño del teléfono.
     * Como todo perfil (Docente, Secretaria, Apoderado) es 1:1 con User,
     * la colección de teléfonos vive UNA sola vez, en el usuario.
     * Estudiante no los usa: al ser menores, la comunicación es con su apoderado.
     */
    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Constructors

    public Telefono() {
    }

    public Telefono(String numero) {
        this();
        this.numero = numero;
    }

    // Getters and Setters

    public String getNumero() {
        return numero;
    }

    public void setNumero(String numero) {
        this.numero = numero;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    // Vive dentro de un Set: requiere equals/hashCode

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Telefono that = (Telefono) o;

        if (getId() != null && that.getId() != null) {
            return getId().equals(that.getId());
        }
        return Objects.equals(numero, that.numero)
                && user != null && user == that.user;
    }

    @Override
    public int hashCode() {
        if (getId() != null) {
            return Objects.hash(getId());
        }
        return Objects.hash(numero, System.identityHashCode(user));
    }

    @Override
    public String toString() {
        return "Telefono{" +
                "id=" + getId() +
                ", numero='" + numero + '\'' +
                '}';
    }
}
