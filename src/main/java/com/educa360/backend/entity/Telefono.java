package com.educa360.backend.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "telefonos")
public class Telefono {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String numero;

    @ManyToOne(optional = false)
    @JoinColumn(name = "apoderado_id", nullable = false)
    private Apoderado apoderado;

}

