package com.unigest.usuario;

import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.csrf.CsrfToken;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class SesionController {

    // Identidad del usuario autenticado.
    @GetMapping("/me")
    public Map<String, Object> usuarioActual(
            Authentication autenticacion) {

        var roles = autenticacion.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .filter(rol -> rol.startsWith("ROLE_"))
                .toList();

        return Map.of(
                "correo", autenticacion.getName(),
                "roles", roles
        );
    }

    // Token CSRF para el futuro inicio de sesion.
    @GetMapping("/csrf")
    public CsrfToken obtenerTokenCsrf(
            CsrfToken csrfToken) {

        return csrfToken;
    }
}
