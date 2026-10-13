package com.educa360.backend.entity;

/**
 * Días en los que puede haber bloques de horario (secundaria: lunes a sábado).
 * Sin acentos: viaja como texto en JSON y en la BD (EnumType.STRING).
 */
public enum DiaSemana {
    LUNES,
    MARTES,
    MIERCOLES,
    JUEVES,
    VIERNES,
    SABADO
}
