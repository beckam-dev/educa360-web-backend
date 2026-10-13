package com.educa360.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Sección (grupo de alumnos) de un período: "3°A".
 *
 * SÓLO SECUNDARIA: el grado va de 1 a 5 y no existe campo "nivel" — el
 * sistema es secundaria, así que la columna sería redundante. Si algún día
 * se amplía a primaria, se relaja la restricción y se añade nivel.
 *
 * Se RE-FORMA en cada período: "3°A" del Bimestre I no es la misma fila que
 * "3°A" del Bimestre II. Eso deja asistencia y matrícula acotadas al período
 * sin necesidad de columnas extra.
 *
 * No hay entidad Aula: "aula" es texto libre ("A-101") para anotar dónde se
 * reúnen. Si más adelante se necesitan horarios o control de capacidad,
 * se extrae Aula como entidad (con su migración).
 */
@Entity
@Table(name = "secciones",
        uniqueConstraints = @UniqueConstraint(columnNames = {"grado", "letra", "periodo_id"}))
public class Seccion extends Auditable {

    @Min(value = 1, message = "El grado va de 1 a 5 (secundaria)")
    @Max(value = 5, message = "El grado va de 1 a 5 (secundaria)")
    @Column(nullable = false)
    private Integer grado;

    @NotBlank
    @Pattern(regexp = "[A-Z]", message = "La letra debe ser una mayúscula (A, B, C...)")
    @Column(nullable = false)
    private String letra;

    @Column
    private String aula; // texto libre: "A-101", "Laboratorio 2"...

    // Docente a cargo del grupo (tutoría). Opcional: una sección puede no tener aún.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tutor_id")
    private Docente tutor;

    // La sección pertenece a UN período (por eso AsignacionDocente no repite periodo).
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "periodo_id", nullable = false)
    private PeriodoAcademico periodo;

    // Constructors

    public Seccion() {
    }

    public Seccion(Integer grado, String letra, String aula, Docente tutor, PeriodoAcademico periodo) {
        this();
        this.grado = grado;
        this.letra = letra;
        this.aula = aula;
        this.tutor = tutor;
        this.periodo = periodo;
    }

    // Helpers

    /** Etiqueta visible para paneles y listados: "3°A". */
    public String getEtiqueta() {
        return grado + "°" + letra;
    }

    // Getters and Setters

    public Integer getGrado() {
        return grado;
    }

    public void setGrado(Integer grado) {
        this.grado = grado;
    }

    public String getLetra() {
        return letra;
    }

    public void setLetra(String letra) {
        this.letra = letra;
    }

    public String getAula() {
        return aula;
    }

    public void setAula(String aula) {
        this.aula = aula;
    }

    public Docente getTutor() {
        return tutor;
    }

    public void setTutor(Docente tutor) {
        this.tutor = tutor;
    }

    public PeriodoAcademico getPeriodo() {
        return periodo;
    }

    public void setPeriodo(PeriodoAcademico periodo) {
        this.periodo = periodo;
    }

    // toString seguro para logs: sin colecciones ni entidades cruzadas

    @Override
    public String toString() {
        return "Seccion{" +
                "id=" + getId() +
                ", grado=" + grado +
                ", letra='" + letra + '\'' +
                ", tutorId=" + (tutor != null ? tutor.getId() : null) +
                ", periodoId=" + (periodo != null ? periodo.getId() : null) +
                '}';
    }
}
