package com.unigest.usuario;

import jakarta.persistence.*;

@Entity
@Table(name = "roles")
public class Rol {
    @Id
    private Long id;
    @Column(nullable = false, unique = true, length = 20)
    private String codigo;
    @Column(nullable = false, length = 60)
    private String nombre;
    protected Rol() { }
    public Long getId() { return id; }
    public String getCodigo() { return codigo; }
}
