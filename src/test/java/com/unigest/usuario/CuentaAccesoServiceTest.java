package com.unigest.usuario;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
import java.util.Optional;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CuentaAccesoServiceTest {
    @Mock UsuarioRepository repo;
    @Mock PasswordEncoder encoder;
    @InjectMocks CuentaAccesoService servicio;

    @Test void emiteClaveAleatoriaSoloParaEstudiante() {
        Usuario u = mock(Usuario.class);
        Rol r = mock(Rol.class);
        when(repo.findById(5L)).thenReturn(Optional.of(u));
        when(u.isActivo()).thenReturn(true);
        when(u.obtenerRoles()).thenReturn(Set.of(r));
        when(r.getCodigo()).thenReturn("ESTUDIANTE");
        when(u.getCorreo()).thenReturn("alumno@ejemplo.test");
        when(encoder.encode(anyString())).thenReturn("hash-seguro");

        var resultado = servicio.emitir(5L);
        assertEquals("alumno@ejemplo.test", resultado.correo());
        assertTrue(resultado.claveTemporal().length() >= 25);
        verify(u).cambiarClave("hash-seguro", true);
        verify(encoder).encode(resultado.claveTemporal());
    }

    @Test void noPermiteEmitirCredencialAdministrativa() {
        Usuario u = mock(Usuario.class);
        Rol r = mock(Rol.class);
        when(repo.findById(1L)).thenReturn(Optional.of(u));
        when(u.isActivo()).thenReturn(true);
        when(u.obtenerRoles()).thenReturn(Set.of(r));
        when(r.getCodigo()).thenReturn("ADMIN");
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> servicio.emitir(1L));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verifyNoInteractions(encoder);
    }

    @Test void contraseñaProvisionalBloqueaAccesoAcademico() {
        Usuario u = mock(Usuario.class);
        when(repo.findByCorreoIgnoreCase("alumno@test.local")).thenReturn(Optional.of(u));
        when(u.isActivo()).thenReturn(true);
        when(u.isRequiereCambioClave()).thenReturn(true);
        assertFalse(servicio.claveDefinitiva("alumno@test.local"));
    }

    @Test void rechazaCambioConClaveActualIncorrecta() {
        Rol r = mock(Rol.class);
        Usuario u = new Usuario("Ana", "Prueba", "ana@test.local", "hash-anterior", r);
        when(repo.findByCorreoIgnoreCase("ana@test.local")).thenReturn(Optional.of(u));
        when(encoder.matches("incorrecta", "hash-anterior")).thenReturn(false);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> servicio.cambiar("ana@test.local", "incorrecta", "NuevaClaveSegura2026"));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(encoder, never()).encode(anyString());
    }

    @Test void cambiarClaveDesactivaRequisitoDeCambio() {
        Rol r = mock(Rol.class);
        Usuario u = new Usuario("Ana", "Prueba", "ana@test.local", "hash-temporal", r);
        u.cambiarClave("hash-temporal", true);
        when(repo.findByCorreoIgnoreCase("ana@test.local")).thenReturn(Optional.of(u));
        when(encoder.matches("temporal", "hash-temporal")).thenReturn(true);
        when(encoder.matches("NuevaClaveSegura2026", "hash-temporal")).thenReturn(false);
        when(encoder.encode("NuevaClaveSegura2026")).thenReturn("hash-definitivo");
        servicio.cambiar("ana@test.local", "temporal", "NuevaClaveSegura2026");
        assertFalse(u.isRequiereCambioClave());
        assertEquals("hash-definitivo", u.obtenerClaveHash());
    }

    @Test void rechazaClavesNuevasDeMenosDeDoceCaracteres() {
        Rol rol = mock(Rol.class);
        Usuario u = new Usuario("Ana", "Prueba", "ana@test.local", "hash-temporal", rol);
        when(repo.findByCorreoIgnoreCase("ana@test.local")).thenReturn(Optional.of(u));
        when(encoder.matches("temporal", "hash-temporal")).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> servicio.cambiar("ana@test.local", "temporal", "Corta2026"));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(encoder, never()).encode(anyString());
    }

    @Test void noEmiteClaveSiCuentaEstaDesactivada() {
        Usuario u = mock(Usuario.class);
        when(repo.findById(5L)).thenReturn(Optional.of(u));
        when(u.isActivo()).thenReturn(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> servicio.emitir(5L));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verifyNoInteractions(encoder);
    }
}
