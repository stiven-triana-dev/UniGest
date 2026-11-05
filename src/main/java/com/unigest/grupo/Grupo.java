package com.unigest.grupo;

import com.unigest.asignatura.Asignatura;
import com.unigest.docente.Docente;
import com.unigest.periodo.Periodo;

import jakarta.persistence.*;

@Entity
@Table(
    name = "grupos",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "grupos_periodo_id_asignatura_id_codigo_key",
            columnNames = {"periodo_id", "asignatura_id", "codigo"}
        )
    }
)
public class Grupo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asignatura_id", nullable = false)
    private Asignatura asignatura;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "docente_id",
        referencedColumnName = "usuario_id",
        nullable = false
    )
    private Docente docente;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "periodo_id", nullable = false)
    private Periodo periodo;

    @Column(nullable = false, length = 15)
    private String codigo;

    @Column(nullable = false)
    private Integer cupo;

    @Column(nullable = false, length = 12)
    private String estado = "BORRADOR";

    protected Grupo() {
    }

    public Grupo(
            Asignatura asignatura,
            Docente docente,
            Periodo periodo,
            String codigo,
            Integer cupo
    ) {
        this.asignatura = asignatura;
        this.docente = docente;
        this.periodo = periodo;
        this.codigo = codigo;
        this.cupo = cupo;
        this.estado = "BORRADOR";
    }

    public Long getId() {
        return id;
    }

    public Asignatura getAsignatura() {
        return asignatura;
    }

    public Docente getDocente() {
        return docente;
    }

    public Periodo getPeriodo() {
        return periodo;
    }

    public String getCodigo() {
        return codigo;
    }

    public Integer getCupo() {
        return cupo;
    }

    public String getEstado() {
        return estado;
    }

    public void actualizarDatos(
            Docente docente,
            String codigo,
            Integer cupo
    ) {
        this.docente = docente;
        this.codigo = codigo;
        this.cupo = cupo;
    }

    public void cambiarEstado(String estado) {
        if (!"BORRADOR".equals(estado)
                && !"ABIERTO".equals(estado)
                && !"CERRADO".equals(estado)) {
            throw new IllegalArgumentException(
                "Estado de grupo no válido"
            );
        }

        this.estado = estado;
    }
}
