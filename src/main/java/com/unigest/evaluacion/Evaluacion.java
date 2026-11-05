package com.unigest.evaluacion;

import com.unigest.grupo.Grupo;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "evaluaciones")
public class Evaluacion {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "grupo_id", nullable = false)
    private Grupo grupo;
    @Column(nullable = false, length = 150)
    private String titulo;
    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal ponderacion;
    private LocalDate fecha;
    @Column(nullable = false)
    private boolean publicada;

    protected Evaluacion() {}
    public Evaluacion(Grupo grupo, String titulo, BigDecimal ponderacion, LocalDate fecha) {
        this.grupo = grupo;
        actualizar(titulo, ponderacion, fecha);
    }
    public void actualizar(String titulo, BigDecimal ponderacion, LocalDate fecha) {
        this.titulo = titulo;
        this.ponderacion = ponderacion;
        this.fecha = fecha;
    }
    public void publicar() { publicada = true; }
    public Long getId() { return id; }
    public Grupo getGrupo() { return grupo; }
    public String getTitulo() { return titulo; }
    public BigDecimal getPonderacion() { return ponderacion; }
    public LocalDate getFecha() { return fecha; }
    public boolean isPublicada() { return publicada; }
}
