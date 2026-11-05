package com.unigest.calificacion;

import com.unigest.usuario.Usuario;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "historial_calificaciones")
public class HistorialCalificacion {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "calificacion_id", nullable = false)
    private Calificacion calificacion;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "modificado_por", nullable = false)
    private Usuario usuario;
    @Column(name = "nota_anterior", nullable = false, precision = 4, scale = 2)
    private BigDecimal anterior;
    @Column(name = "nota_nueva", nullable = false, precision = 4, scale = 2)
    private BigDecimal nueva;
    @Column(nullable = false, length = 255)
    private String motivo;
    protected HistorialCalificacion() {}
    public HistorialCalificacion(Calificacion c, Usuario usuario,
            BigDecimal anterior, BigDecimal nueva, String motivo) {
        this.calificacion = c; this.usuario = usuario;
        this.anterior = anterior; this.nueva = nueva; this.motivo = motivo;
    }
}
