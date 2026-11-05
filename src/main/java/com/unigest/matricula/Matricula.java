package com.unigest.matricula;

import com.unigest.estudiante.Estudiante;
import com.unigest.grupo.Grupo;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "matriculas")
public class Matricula {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "estudiante_id",
        referencedColumnName = "usuario_id",
        nullable = false
    )
    private Estudiante estudiante;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "grupo_id", nullable = false)
    private Grupo grupo;

    @Column(name = "fecha", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime fecha;

    @Column(nullable = false, length = 15)
    private String estado = "ACTIVA";

    protected Matricula() {
    }

    public Matricula(Estudiante estudiante, Grupo grupo) {
        this.estudiante = estudiante;
        this.grupo = grupo;
        this.estado = "ACTIVA";
    }

    public Long getId() {
        return id;
    }

    public Estudiante getEstudiante() {
        return estudiante;
    }

    public Grupo getGrupo() {
        return grupo;
    }

    public OffsetDateTime getFecha() {
        return fecha;
    }

    public String getEstado() {
        return estado;
    }

    public void retirar() {
        if (!"ACTIVA".equals(estado)) {
            throw new IllegalStateException(
                "Solo se pueden retirar matriculas activas"
            );
        }

        this.estado = "RETIRADA";
    }

    public void reactivar() {
        if (!"RETIRADA".equals(estado)) {
            throw new IllegalStateException(
                "Solo se pueden reactivar matriculas retiradas"
            );
        }

        this.estado = "ACTIVA";
    }

    public void finalizar() {
        if (!"ACTIVA".equals(estado)) {
            throw new IllegalStateException(
                "Solo se pueden finalizar matriculas activas"
            );
        }

        this.estado = "FINALIZADA";
    }
}
