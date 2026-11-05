package com.unigest.calificacion;

import com.unigest.evaluacion.Evaluacion;
import com.unigest.grupo.Grupo;
import com.unigest.matricula.Matricula;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "calificaciones")
public class Calificacion {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "matricula_id", nullable = false)
    private Matricula matricula;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "evaluacion_id", nullable = false)
    private Evaluacion evaluacion;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "grupo_id", nullable = false)
    private Grupo grupo;
    @Column(nullable = false, precision = 4, scale = 2)
    private BigDecimal nota;
    @Column(name = "actualizado_en", nullable = false)
    private OffsetDateTime actualizadoEn = OffsetDateTime.now();

    protected Calificacion() {}
    public Calificacion(Matricula matricula, Evaluacion evaluacion, BigDecimal nota) {
        this.matricula = matricula;
        this.evaluacion = evaluacion;
        this.grupo = evaluacion.getGrupo();
        actualizar(nota);
    }
    public void actualizar(BigDecimal nueva) {
        this.nota = nueva;
        this.actualizadoEn = OffsetDateTime.now();
    }
    public Long getId() { return id; }
    public Matricula getMatricula() { return matricula; }
    public Evaluacion getEvaluacion() { return evaluacion; }
    public BigDecimal getNota() { return nota; }
}
