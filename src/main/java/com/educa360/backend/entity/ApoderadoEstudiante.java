package com.educa360.backend.entity;

import jakarta.persistence.*;

@Entity
@Table(
        name = "apoderado_estudiante",
        uniqueConstraints = {
                @UniqueConstraint(
                        columnNames = {"apoderado_id", "estudiante_id"}
                )
        }
)
public class ApoderadoEstudiante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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

    public Long getId() {
        return id;
    }

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
}
