package com.unigest.estudiante;

import com.unigest.programa.Programa;
import com.unigest.usuario.Usuario;
import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "estudiantes")
public class Estudiante {
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

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "programa_id", nullable = false)
    private Programa programa;

    @Column(name = "fecha_ingreso", nullable = false)
    private LocalDate fechaIngreso;

    protected Estudiante() { }
    public Estudiante(Usuario usuario, String codigo, String documento, Programa programa, LocalDate fechaIngreso) {
        this.usuario = usuario;
        actualizarDatos(codigo, documento, programa);
        this.fechaIngreso = fechaIngreso;
    }
    public Long getId() { return id; }
    public Usuario getUsuario() { return usuario; }
    public String getCodigo() { return codigo; }
    public String getDocumento() { return documento; }
    public Programa getPrograma() { return programa; }
    public LocalDate getFechaIngreso() { return fechaIngreso; }
    public void actualizarDatos(String codigo, String documento, Programa programa) {
        this.codigo = codigo;
        this.documento = documento;
        this.programa = programa;
    }
}
