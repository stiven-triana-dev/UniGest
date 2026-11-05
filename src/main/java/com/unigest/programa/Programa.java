package com.unigest.programa;

import jakarta.persistence.*;

@Entity
@Table(name = "programas")
public class Programa {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 20, unique = true)
    private String codigo;
    @Column(nullable = false, length = 150)
    private String nombre;
    @Column(nullable = false)
    private boolean activo;

    protected Programa() { }
    public Long getId() { return id; }
    public String getCodigo() { return codigo; }
    public String getNombre() { return nombre; }
    public boolean isActivo() { return activo; }
}
