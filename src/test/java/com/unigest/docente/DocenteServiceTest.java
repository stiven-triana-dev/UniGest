package com.unigest.docente;

import com.unigest.usuario.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocenteServiceTest {

    @Mock
    private DocenteRepository docentes;

    @Mock
    private UsuarioRepository usuarios;

    @Mock
    private RolRepository roles;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private Rol rol;

    @InjectMocks
    private DocenteService servicio;

    // Datos ficticios para las pruebas.
    private DocenteRequest solicitud() {
        return new DocenteRequest(
                "Ana",
                "Pérez",
                "ANA@UNIGEST.LOCAL",
                "DOC-01",
                "CED-001",
                LocalDate.of(2025, 1, 15)
        );
    }

    private Docente docente() {
        Usuario usuario = new Usuario(
                "Ana", "Pérez", "ana@unigest.local",
                "hash", rol
        );

        return new Docente(
                usuario, "DOC-01", "CED-001",
                LocalDate.of(2025, 1, 15)
        );
    }

    // 1. Registrar un docente.
    @Test
    void registraDocenteCorrectamente() {
        when(roles.findByCodigo("DOCENTE"))
                .thenReturn(Optional.of(rol));

        when(passwordEncoder.encode(anyString()))
                .thenReturn("hash");

        when(usuarios.save(any(Usuario.class)))
                .thenAnswer(i -> i.getArgument(0));

        when(docentes.save(any(Docente.class)))
                .thenAnswer(i -> i.getArgument(0));

        DocenteResponse resultado = servicio.crear(solicitud());

        assertEquals("ana@unigest.local", resultado.correo());
        assertEquals("DOC-01", resultado.codigo());
        assertEquals("CED-001", resultado.documento());

        verify(usuarios).save(any(Usuario.class));
        verify(docentes).save(any(Docente.class));
    }

    // 2. Rechazar correos duplicados.
    @Test
    void rechazaCorreoDuplicado() {
        when(usuarios.existsByCorreoIgnoreCase("ana@unigest.local"))
                .thenReturn(true);

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> servicio.crear(solicitud())
        );

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());

        verify(usuarios, never()).save(any(Usuario.class));
        verify(docentes, never()).save(any(Docente.class));
    }

    // 3. Consultar un docente existente.
    @Test
    void consultaDocenteExistente() {
        when(docentes.findById(1L))
                .thenReturn(Optional.of(docente()));

        DocenteResponse resultado = servicio.consultar(1L);

        assertEquals("DOC-01", resultado.codigo());
        assertEquals("Ana", resultado.nombres());
    }

    // 4. Actualizar datos conservando la fecha original.
    @Test
    void actualizaDatosSinCambiarFecha() {
        Docente actual = docente();

        when(docentes.findById(1L))
                .thenReturn(Optional.of(actual));

        DocenteRequest cambios = new DocenteRequest(
                "Laura",
                "Gómez",
                "laura@unigest.local",
                "DOC-02",
                "CED-002",
                LocalDate.of(2026, 1, 1)
        );

        DocenteResponse resultado = servicio.actualizar(1L, cambios);

        assertEquals("Laura", resultado.nombres());
        assertEquals("DOC-02", resultado.codigo());
        assertEquals("CED-002", resultado.documento());

        assertEquals(
                LocalDate.of(2025, 1, 15),
                resultado.fechaVinculacion()
        );
    }

    // 5. Desactivar un docente sin eliminarlo.
    @Test
    void desactivaDocenteSinEliminarlo() {
        Docente actual = docente();

        when(docentes.findById(1L))
                .thenReturn(Optional.of(actual));

        servicio.desactivar(1L);

        assertFalse(actual.getUsuario().isActivo());

        verify(docentes, never())
                .delete(any(Docente.class));
    }

    // 6. Mostrar solamente docentes activos.
    @Test
    void listaSolamenteDocentesActivos() {
        Docente activo = docente();
        Docente inactivo = docente();

        inactivo.getUsuario().desactivar();

        when(docentes.findAll(any(Sort.class)))
                .thenReturn(List.of(activo, inactivo));

        List<DocenteResponse> resultado = servicio.listar();

        assertEquals(1, resultado.size());
        assertEquals("DOC-01", resultado.get(0).codigo());
    }
}
