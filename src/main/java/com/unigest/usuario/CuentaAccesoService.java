package com.unigest.usuario;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Credenciales individuales: no usa contraseñas universales ni las registra en logs. */
@Service("cuentaAccesoService")
public class CuentaAccesoService {
    private static final SecureRandom ALEATORIO = new SecureRandom();
    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;

    public CuentaAccesoService(UsuarioRepository usuarios, PasswordEncoder encoder) {
        this.usuarios = usuarios;
        this.encoder = encoder;
    }


    // Demo pública: protege las cuentas de prueba. Opcional para las pruebas unitarias.
    private com.unigest.demo.DemoModo demo;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    void setDemo(com.unigest.demo.DemoModo demo) { this.demo = demo; }

    private void protegerDemo(String correo) { if (demo != null) demo.protegerCuenta(correo); }

    public record CredencialTemporal(String correo, String claveTemporal) { }
    public record EstadoClave(boolean requiereCambio) { }

    @Transactional
    public CredencialTemporal emitir(Long usuarioId) {
        Usuario u = usuarios.findById(usuarioId)
            .filter(Usuario::isActivo)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Cuenta activa no encontrada"));
        boolean academico = u.obtenerRoles().stream().anyMatch(r ->
            "DOCENTE".equals(r.getCodigo()) || "ESTUDIANTE".equals(r.getCodigo()));
        boolean administrador = u.obtenerRoles().stream().anyMatch(r ->
            "ADMIN".equals(r.getCodigo()));
        protegerDemo(u.getCorreo());
        if (!academico || administrador) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "Esta operación solo está disponible para docentes y estudiantes");
        }
        byte[] bytes = new byte[24];
        ALEATORIO.nextBytes(bytes);
        String temporal = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        u.cambiarClave(encoder.encode(temporal), true);
        // Única respuesta con el secreto. Nunca se almacena el secreto en texto claro.
        return new CredencialTemporal(u.getCorreo(), temporal);
    }

    @Transactional(readOnly = true)
    public boolean requiereCambio(String correo) {
        return buscarActivo(correo).isRequiereCambioClave();
    }

    @Transactional(readOnly = true)
    public boolean claveDefinitiva(String correo) {
        return !requiereCambio(correo);
    }

    @Transactional
    public void cambiar(String correo, String actual, String nueva) {
        Usuario u = buscarActivo(correo);
        protegerDemo(u.getCorreo());
        if (!encoder.matches(actual, obtenerHash(u))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "La contraseña actual no coincide");
        }
        if (nueva == null || nueva.length() < 12 ||
            nueva.getBytes(StandardCharsets.UTF_8).length > 72 ||
            nueva.isBlank() || encoder.matches(nueva, obtenerHash(u))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "La nueva contraseña debe ser distinta y tener entre 12 y 72 bytes");
        }
        u.cambiarClave(encoder.encode(nueva), false);
    }

    private Usuario buscarActivo(String correo) {
        return usuarios.findByCorreoIgnoreCase(correo).filter(Usuario::isActivo)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                "Cuenta no disponible"));
    }

    // El hash es accesible solo dentro del módulo de usuarios.
    private String obtenerHash(Usuario usuario) { return usuario.obtenerClaveHash(); }
}
