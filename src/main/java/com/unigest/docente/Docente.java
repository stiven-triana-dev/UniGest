
package com.unigest.docente;

import com.unigest.usuario.Usuario;
import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "docentes")
public class Docente {

    @Id
    @Column(name = "usuario_id")
    private Long id;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(nullable = false, unique = true, length = 30)
    private String codigo;

    @Column(nullable = false, unique = true, length = 30)
    private String documento;

    @Column(name = "fecha_vinculacion", nullable = false)
    private LocalDate fechaVinculacion;

    protected Docente() {
    }

    public Docente(Usuario usuario, String codigo,
                   String documento, LocalDate fechaVinculacion) {
        this.usuario = usuario;
        this.codigo = codigo;
        this.documento = documento;
        this.fechaVinculacion = fechaVinculacion;
    }

    public Long getId() {
        return id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getDocumento() {
        return documento;
    }

    public LocalDate getFechaVinculacion() {
        return fechaVinculacion;
    }

    public void actualizarDatos(String codigo, String documento) {
        this.codigo = codigo;
        this.documento = documento;
    }
}
