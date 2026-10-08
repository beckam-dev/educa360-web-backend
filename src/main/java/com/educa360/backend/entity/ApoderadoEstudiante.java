package com.educa360.backend.entity;

import jakarta.persistence.*;

import java.util.Objects;

@Entity
@Table(
        name = "apoderado_estudiante",
        uniqueConstraints = {
                @UniqueConstraint(
                        columnNames = {"apoderado_id", "estudiante_id"}
                )
        }
)
public class ApoderadoEstudiante extends Auditable {

    @ManyToOne(optional = false)
    @JoinColumn(name = "apoderado_id", nullable = false)
    private Apoderado apoderado;

    @ManyToOne(optional = false)
    @JoinColumn(name = "estudiante_id", nullable = false)
    private Estudiante estudiante;

    public enum Parentesco {
        PADRE,
        MADRE,
        TUTOR,
        HERMANO,
        OTRO
    }

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Parentesco parentesco;

    /*
     * Permite identificar al responsable principal
     * cuando existen varios apoderados.
     */
    @Column(nullable = false)
    private boolean responsablePrincipal;

    /*
     * Permite desactivar la relación sin eliminar
     * el historial.
     */
    @Column(nullable = false)
    private boolean activo = true;

    // Constructors

    public ApoderadoEstudiante() {
    }

    public ApoderadoEstudiante(
            Apoderado apoderado,
            Estudiante estudiante,
            Parentesco parentesco,
            boolean responsablePrincipal
    ) {
        this.apoderado = apoderado;
        this.estudiante = estudiante;
        this.parentesco = parentesco;
        this.responsablePrincipal = responsablePrincipal;
    }

    // Getters and Setters

    public Apoderado getApoderado() {
        return apoderado;
    }

    public void setApoderado(Apoderado apoderado) {
        this.apoderado = apoderado;
    }

    public Estudiante getEstudiante() {
        return estudiante;
    }

    public void setEstudiante(Estudiante estudiante) {
        this.estudiante = estudiante;
    }

    public Parentesco getParentesco() {
        return parentesco;
    }

    public void setParentesco(Parentesco parentesco) {
        this.parentesco = parentesco;
    }

    public boolean isResponsablePrincipal() {
        return responsablePrincipal;
    }

    public void setResponsablePrincipal(boolean responsablePrincipal) {
        this.responsablePrincipal = responsablePrincipal;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    /*
     * Vive dentro de un Set, así que necesita equals/hashCode.
     *
     * Clave de negocio ESTABLE: apoderado + estudiante, fijados en el
     * constructor y que nunca cambian. NO se usa el id: el id pasa de
     * null a un valor al persistir, y si el hashCode cambiara después de
     * insertar el elemento en el Set, el Set "perdería" el elemento y
     * quitarEstudiante()/orphanRemoval dejarían de funcionar.
     */

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ApoderadoEstudiante that = (ApoderadoEstudiante) o;
        return apoderado != null && apoderado == that.apoderado
                && estudiante != null && estudiante == that.estudiante;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                System.identityHashCode(apoderado),
                System.identityHashCode(estudiante)
        );
    }

    @Override
    public String toString() {
        return "ApoderadoEstudiante{" +
                "id=" + getId() +
                ", apoderadoId=" + (apoderado != null ? apoderado.getId() : null) +
                ", estudianteId=" + (estudiante != null ? estudiante.getId() : null) +
                ", parentesco=" + parentesco +
                ", responsablePrincipal=" + responsablePrincipal +
                ", activo=" + activo +
                '}';
    }
}
