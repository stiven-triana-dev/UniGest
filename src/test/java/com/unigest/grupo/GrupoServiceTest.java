package com.unigest.grupo;

import com.unigest.asignatura.Asignatura;
import com.unigest.asignatura.AsignaturaRepository;
import com.unigest.docente.Docente;
import com.unigest.docente.DocenteRepository;
import com.unigest.periodo.Periodo;
import com.unigest.periodo.PeriodoRepository;
import com.unigest.usuario.Usuario;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GrupoServiceTest {

    @Mock
    private GrupoRepository grupos;

    @Mock
    private AsignaturaRepository asignaturas;

    @Mock
    private DocenteRepository docentes;

    @Mock
    private PeriodoRepository periodos;

    @InjectMocks
    private GrupoService servicio;

    // Datos ficticios para las pruebas.
    private GrupoRequest solicitud() {
        return new GrupoRequest(
                1L,
                7L,
                1L,
                "PROG101-G01",
                30
        );
    }

    private Asignatura asignatura() {
        Asignatura asignatura = mock(Asignatura.class);

        when(asignatura.getId()).thenReturn(1L);
        when(asignatura.getNombre())
                .thenReturn("Fundamentos de Programacion");

        // La asignatura debe estar activa.
        when(asignatura.isActiva()).thenReturn(true);

        return asignatura;
    }

    private Docente docenteActivo() {
        Docente docente = mock(Docente.class);
        Usuario usuario = mock(Usuario.class);

        when(docente.getId()).thenReturn(7L);
        when(docente.getUsuario()).thenReturn(usuario);
        when(usuario.isActivo()).thenReturn(true);
        when(usuario.getNombres()).thenReturn("Laura");
        when(usuario.getApellidos()).thenReturn("Gomez");

        return docente;
    }

    private Periodo periodo() {
        Periodo periodo = mock(Periodo.class);

        when(periodo.getId()).thenReturn(1L);
        when(periodo.getNombre())
                .thenReturn("Primer semestre de 2027");

        return periodo;
    }

    // 1. Registrar correctamente un grupo.
    @Test
    void registraGrupoCorrectamente() {
        Asignatura asignatura = asignatura();
        Docente docente = docenteActivo();
        Periodo periodo = periodo();

        when(asignaturas.findById(1L))
                .thenReturn(Optional.of(asignatura));

        when(docentes.findById(7L))
                .thenReturn(Optional.of(docente));

        when(periodos.findById(1L))
                .thenReturn(Optional.of(periodo));

        when(grupos.save(any(Grupo.class)))
                .thenAnswer(i -> i.getArgument(0));

        GrupoResponse resultado = servicio.crear(solicitud());

        assertEquals("PROG101-G01", resultado.codigo());
        assertEquals(30, resultado.cupo());
        assertEquals("BORRADOR", resultado.estado());
        assertEquals(7L, resultado.docenteId());

        verify(grupos).save(any(Grupo.class));
    }

    // 2. Rechazar un codigo duplicado.
    @Test
    void rechazaCodigoDuplicado() {
        Asignatura asignatura = mock(Asignatura.class);
        Docente docente = mock(Docente.class);
        Periodo periodo = mock(Periodo.class);
        Usuario usuario = mock(Usuario.class);

        when(asignaturas.findById(1L))
                .thenReturn(Optional.of(asignatura));

        when(asignatura.isActiva()).thenReturn(true);
        when(asignatura.getId()).thenReturn(1L);

        when(docentes.findById(7L))
                .thenReturn(Optional.of(docente));

        when(docente.getUsuario()).thenReturn(usuario);
        when(usuario.isActivo()).thenReturn(true);

        when(periodos.findById(1L))
                .thenReturn(Optional.of(periodo));

        when(periodo.getId()).thenReturn(1L);

        when(grupos.existsByPeriodoIdAndAsignaturaIdAndCodigo(
                1L, 1L, "PROG101-G01"))
                .thenReturn(true);

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> servicio.crear(solicitud())
        );

        assertEquals(
                HttpStatus.CONFLICT,
                error.getStatusCode()
        );

        verify(grupos, never()).save(any(Grupo.class));
    }

    // 3. Rechazar cupos superiores al limite.
    @Test
    void rechazaCupoInvalido() {
        GrupoRequest solicitud = new GrupoRequest(
                1L, 7L, 1L, "PROG101-G02", 1001
        );

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> servicio.crear(solicitud)
        );

        assertEquals(
                HttpStatus.BAD_REQUEST,
                error.getStatusCode()
        );

        verify(grupos, never()).save(any(Grupo.class));
    }

    // 4. Impedir asignar un docente inactivo.
    @Test
    void rechazaDocenteInactivo() {
        Asignatura asignatura = mock(Asignatura.class);
        Docente docente = mock(Docente.class);
        Usuario usuario = mock(Usuario.class);

        when(asignaturas.findById(1L))
                .thenReturn(Optional.of(asignatura));

        when(asignatura.isActiva()).thenReturn(true);

        when(docentes.findById(7L))
                .thenReturn(Optional.of(docente));

        when(docente.getUsuario()).thenReturn(usuario);
        when(usuario.isActivo()).thenReturn(false);

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> servicio.crear(solicitud())
        );

        assertEquals(
                HttpStatus.NOT_FOUND,
                error.getStatusCode()
        );

        verify(grupos, never()).save(any(Grupo.class));
    }

    // 5. Actualizar el cupo de un grupo existente.
    @Test
    void actualizaCupoCorrectamente() {
        Asignatura asignatura = mock(Asignatura.class);
        Docente docente = docenteActivo();
        Periodo periodo = mock(Periodo.class);

        when(asignatura.getId()).thenReturn(1L);
        when(asignatura.getNombre())
                .thenReturn("Fundamentos de Programacion");

        when(periodo.getId()).thenReturn(1L);
        when(periodo.getNombre())
                .thenReturn("Primer semestre de 2027");

        Grupo grupo = new Grupo(
                asignatura,
                docente,
                periodo,
                "PROG101-G01",
                30
        );

        when(grupos.findById(1L))
                .thenReturn(Optional.of(grupo));

        when(docentes.findById(7L))
                .thenReturn(Optional.of(docente));

        GrupoRequest cambios = new GrupoRequest(
                1L,
                7L,
                1L,
                "PROG101-G01",
                35
        );

        GrupoResponse resultado =
                servicio.actualizar(1L, cambios);

        assertEquals(35, resultado.cupo());
        assertEquals("PROG101-G01", resultado.codigo());
        assertEquals("BORRADOR", resultado.estado());
    }

    // 6. Cambiar el grupo de BORRADOR a ABIERTO.
    @Test
    void abreGrupoCorrectamente() {
        Asignatura asignatura = mock(Asignatura.class);
        Docente docente = docenteActivo();
        Periodo periodo = mock(Periodo.class);

        when(asignatura.getId()).thenReturn(1L);
        when(asignatura.getNombre())
                .thenReturn("Fundamentos de Programacion");

        when(periodo.getId()).thenReturn(1L);
        when(periodo.getNombre())
                .thenReturn("Primer semestre de 2027");

        Grupo grupo = new Grupo(
                asignatura,
                docente,
                periodo,
                "PROG101-G01",
                35
        );

        when(grupos.findById(1L))
                .thenReturn(Optional.of(grupo));

        GrupoResponse resultado =
                servicio.cambiarEstado(1L, "ABIERTO");

        assertEquals("ABIERTO", resultado.estado());
        assertEquals("ABIERTO", grupo.getEstado());
    }

    // 7. Impedir reabrir un grupo cerrado.
    @Test
    void rechazaReabrirGrupoCerrado() {
        Grupo grupo = mock(Grupo.class);

        when(grupos.findById(1L))
                .thenReturn(Optional.of(grupo));

        when(grupo.getEstado()).thenReturn("CERRADO");

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> servicio.cambiarEstado(1L, "ABIERTO")
        );

        assertEquals(
                HttpStatus.CONFLICT,
                error.getStatusCode()
        );

        verify(grupo, never()).cambiarEstado(anyString());
    }
}
