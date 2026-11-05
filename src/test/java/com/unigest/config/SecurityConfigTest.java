package com.unigest.config;

import com.unigest.usuario.Rol;
import com.unigest.usuario.Usuario;
import com.unigest.usuario.UsuarioRepository;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SecurityConfigTest {

    // 1. Cargar administrador desde PostgreSQL (simulado).
    @Test
    void debeCargarAdministradorDesdeRepositorio() {

        UsuarioRepository repositorio = mock(UsuarioRepository.class);
        Rol rol = mock(Rol.class);

        when(rol.getCodigo()).thenReturn("ADMIN");

        Usuario administrador = new Usuario(
                "Administrador",
                "UniGest",
                "admin@unigest.local",
                "hash-simulado-para-prueba",
                rol
        );

        when(repositorio.findByCorreoIgnoreCase("admin@unigest.local"))
                .thenReturn(Optional.of(administrador));

        var servicio = new SecurityConfig()
                .usuarioAutenticacionService(repositorio);

        var usuario = servicio.loadUserByUsername(
                "admin@unigest.local"
        );

        assertEquals("admin@unigest.local", usuario.getUsername());

        assertTrue(usuario.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));

        assertTrue(usuario.isEnabled());
    }

    // 2. Rechazar usuarios que no existen.
    @Test
    void debeRechazarUsuarioInexistente() {

        UsuarioRepository repositorio = mock(UsuarioRepository.class);

        when(repositorio.findByCorreoIgnoreCase("noexiste@unigest.local"))
                .thenReturn(Optional.empty());

        var servicio = new SecurityConfig()
                .usuarioAutenticacionService(repositorio);

        assertThrows(
                UsernameNotFoundException.class,
                () -> servicio.loadUserByUsername("noexiste@unigest.local")
        );
    }

    // 3. Impedir el acceso de usuarios inactivos.
    @Test
    void debeDeshabilitarUsuarioInactivo() {

        UsuarioRepository repositorio = mock(UsuarioRepository.class);
        Rol rol = mock(Rol.class);

        when(rol.getCodigo()).thenReturn("ESTUDIANTE");

        Usuario estudiante = new Usuario(
                "Ana",
                "Perez",
                "ana@unigest.local",
                "hash-simulado-para-prueba",
                rol
        );

        estudiante.desactivar();

        when(repositorio.findByCorreoIgnoreCase("ana@unigest.local"))
                .thenReturn(Optional.of(estudiante));

        var servicio = new SecurityConfig()
                .usuarioAutenticacionService(repositorio);

        var usuario = servicio.loadUserByUsername(
                "ana@unigest.local"
        );

        assertFalse(usuario.isEnabled());
    }
}
