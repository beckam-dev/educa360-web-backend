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

    /*
     * Vive dentro de un Set: equals/hashCode con clave de negocio estable
     * (dueño + número), SIN el id (el id cambia al persistir y rompería
     * la búsqueda dentro del Set). La restricción unique(user_id, numero)
     * de la BD es la garantía definitiva contra duplicados.
     *
     * Ojo: si se cambia el número de un teléfono ya guardado, hay que
     * quitarlo y volver a agregarlo al Set (el hashCode depende de él).
     */

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Telefono that = (Telefono) o;
        return user != null && user == that.user
                && Objects.equals(numero, that.numero);
    }

    @Override
    public int hashCode() {
        return Objects.hash(System.identityHashCode(user), numero);
    }

    @Override
    public String toString() {
        return "Telefono{" +
                "id=" + getId() +
                ", numero='" + numero + '\'' +
                '}';
    }
}
