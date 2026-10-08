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
     * equals/hashCode son obligatorios porque esta entidad vive
     * dentro de un Set. Sin ellos, Hibernate permitiría duplicados
     * silenciosos de la misma pareja apoderado-estudiante.
     *
     * Persistido: se compara por id.
     * Transitorio: se compara por la pareja (referencias).
     */

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ApoderadoEstudiante that = (ApoderadoEstudiante) o;

        if (getId() != null && that.getId() != null) {
            return getId().equals(that.getId());
        }
        return apoderado != null && apoderado == that.apoderado
                && estudiante != null && estudiante == that.estudiante;
    }

    @Override
    public int hashCode() {
        if (getId() != null) {
            return Objects.hash(getId());
        }
        return Objects.hash(
                System.identityHashCode(apoderado),
                System.identityHashCode(estudiante)
        );
    }
}
