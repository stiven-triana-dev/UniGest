package com.unigest.asignatura;

import jakarta.persistence.*;

@Entity
@Table(name = "asignaturas")
public class Asignatura {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 20, unique = true)
    private String codigo;
    @Column(nullable = false, length = 150)
    private String nombre;
    @Column(nullable = false)
    private short creditos;
    @Column(nullable = false)
    private boolean activa;

    protected Asignatura() { }
    public Long getId() { return id; }
    public String getCodigo() { return codigo; }
    public String getNombre() { return nombre; }
    public short getCreditos() { return creditos; }
    public boolean isActiva() { return activa; }
}
