package com.unigest.usuario;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "usuarios")
public class Usuario {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 100)
    private String nombres;
    @Column(nullable = false, length = 100)
    private String apellidos;
    @Column(nullable = false, length = 180)
    private String correo;
    @Column(name = "clave_hash", nullable = false, length = 255)
    private String claveHash;
    @Column(nullable = false)
    private boolean activo = true;
    @Column(name = "actualizado_en", nullable = false)
    private OffsetDateTime actualizadoEn = OffsetDateTime.now();

    @Column(name = "requiere_cambio_clave", nullable = false)
    private boolean requiereCambioClave = false;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "usuario_roles", joinColumns = @JoinColumn(name = "usuario_id"),
            inverseJoinColumns = @JoinColumn(name = "rol_id"))
    private Set<Rol> roles = new HashSet<>();

    protected Usuario() { }
    public Usuario(String nombres, String apellidos, String correo, String claveHash, Rol rol) {
        actualizarDatos(nombres, apellidos, correo);
        this.claveHash = claveHash;
        this.roles.add(rol);
    }
    public Long getId() { return id; }
    public String getNombres() { return nombres; }
    public String getApellidos() { return apellidos; }
    public String getCorreo() { return correo; }
    public boolean isActivo() { return activo; }
    public boolean isRequiereCambioClave() { return requiereCambioClave; }
    public void cambiarClave(String hash, boolean provisional) {
        this.claveHash = hash;
        this.requiereCambioClave = provisional;
        this.actualizadoEn = OffsetDateTime.now();
    }

    public void actualizarDatos(String nombres, String apellidos, String correo) {
        this.nombres = nombres;
        this.apellidos = apellidos;
        this.correo = correo;
        this.actualizadoEn = OffsetDateTime.now();
    }
    public void desactivar() { this.activo = false; this.actualizadoEn = OffsetDateTime.now(); }

// Consultar contraseña para autenticación
String obtenerClaveHash() {
    return claveHash;
}

// Consultar los roles del usuario
Set<Rol> obtenerRoles() {
    return Set.copyOf(roles);
}
}