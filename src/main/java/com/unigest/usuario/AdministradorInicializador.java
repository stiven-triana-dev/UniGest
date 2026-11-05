
package com.unigest.usuario;

import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdministradorInicializador implements ApplicationRunner {

    private static final Logger log =
            LoggerFactory.getLogger(AdministradorInicializador.class);

    private final UsuarioRepository usuarios;
    private final RolRepository roles;
    private final PasswordEncoder encoder;

    @Value("${UNIGEST_ADMIN_EMAIL:}")
    private String correo;

    @Value("${UNIGEST_ADMIN_PASSWORD:}")
    private String clave;

    public AdministradorInicializador(
            UsuarioRepository usuarios,
            RolRepository roles,
            PasswordEncoder encoder) {
        this.usuarios = usuarios;
        this.roles = roles;
        this.encoder = encoder;
    }

    @Override
    public void run(ApplicationArguments args) {

        // No crear cuentas si no se configura el administrador inicial.
        if (correo.isBlank() && clave.isBlank()) {
            return;
        }

        if (correo.isBlank() || clave.isBlank() || clave.length() < 12) {
            throw new IllegalStateException(
                    "Configura un correo y una contraseña de 12 o más caracteres");
        }

        String email = correo.trim().toLowerCase(Locale.ROOT);

        var existente = usuarios.findByCorreoIgnoreCase(email);

        if (existente.isPresent()) {
            boolean esAdministrador = existente.get()
                    .obtenerRoles()
                    .stream()
                    .anyMatch(rol -> "ADMIN".equals(rol.getCodigo()));

            if (!esAdministrador) {
                throw new IllegalStateException(
                        "El correo ya pertenece a otra cuenta");
            }

            log.info("La cuenta administrativa ya existe. No se modifica.");
            return;
        }

        Rol rolAdmin = roles.findByCodigo("ADMIN")
                .orElseThrow(() -> new IllegalStateException(
                        "No existe el rol ADMIN"));

        Usuario administrador = new Usuario(
                "Administrador",
                "UniGest",
                email,
                encoder.encode(clave),
                rolAdmin
        );

        usuarios.save(administrador);

        log.info("Cuenta administrativa inicial creada correctamente");
    }
}
