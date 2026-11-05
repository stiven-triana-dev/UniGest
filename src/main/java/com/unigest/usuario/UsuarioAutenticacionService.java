
package com.unigest.usuario;

import java.util.Locale;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

public class UsuarioAutenticacionService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioAutenticacionService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String correo)
            throws UsernameNotFoundException {

        if (correo == null || correo.isBlank()) {
            throw new UsernameNotFoundException("Credenciales incorrectas");
        }

        Usuario usuario = usuarioRepository
                .findByCorreoIgnoreCase(correo.trim())
                .orElseThrow(() ->
                        new UsernameNotFoundException("Credenciales incorrectas"));

        String[] permisos = usuario.obtenerRoles().stream()
                .map(rol -> {
                    String codigo = rol.getCodigo()
                            .trim()
                            .toUpperCase(Locale.ROOT);

                    return codigo.startsWith("ROLE_")
                            ? codigo
                            : "ROLE_" + codigo;
                })
                .toArray(String[]::new);

        return User.withUsername(usuario.getCorreo())
                .password(usuario.obtenerClaveHash())
                .disabled(!usuario.isActivo())
                .authorities(permisos)
                .build();
    }
}
