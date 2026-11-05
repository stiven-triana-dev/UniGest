
package com.unigest.matricula;

import com.unigest.estudiante.Estudiante;
import com.unigest.estudiante.EstudianteRepository;
import com.unigest.grupo.Grupo;
import com.unigest.grupo.GrupoRepository;
import com.unigest.usuario.Usuario;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MatriculaServiceTest {

    @Mock private MatriculaRepository matriculas;
    @Mock private EstudianteRepository estudiantes;
    @Mock private GrupoRepository grupos;
    @Mock private EntityManager entityManager;

    @Mock private Grupo grupo;
    @Mock private Estudiante estudiante;
    @Mock private Usuario usuario;

    private MatriculaService service;

    private static final Long ESTUDIANTE_ID = 10L;
    private static final Long GRUPO_ID = 1L;

    @BeforeEach
    void preparar() {
        service = new MatriculaService(
                matriculas,
                estudiantes,
                grupos,
                entityManager
        );
    }

    private void prepararGrupoAbierto() {
        when(entityManager.find(
                Grupo.class,
                GRUPO_ID,
                LockModeType.PESSIMISTIC_WRITE
        )).thenReturn(grupo);

        when(grupo.getEstado()).thenReturn("ABIERTO");
    }

    private void prepararEstudianteActivo() {
        when(estudiantes.findById(ESTUDIANTE_ID))
                .thenReturn(Optional.of(estudiante));

        when(estudiante.getUsuario()).thenReturn(usuario);
        when(usuario.isActivo()).thenReturn(true);
        when(estudiante.getId()).thenReturn(ESTUDIANTE_ID);
        when(grupo.getId()).thenReturn(GRUPO_ID);
    }

    @Test
    void rechazaGrupoCerrado() {
        when(entityManager.find(
                Grupo.class,
                GRUPO_ID,
                LockModeType.PESSIMISTIC_WRITE
        )).thenReturn(grupo);

        when(grupo.getEstado()).thenReturn("CERRADO");

        assertThrows(
                ResponseStatusException.class,
                () -> service.matricular(
                        new MatriculaRequest(
                                ESTUDIANTE_ID,
                                GRUPO_ID
                        )
                )
        );

        verify(matriculas, never()).saveAndFlush(any());
    }

    @Test
    void rechazaGrupoInexistente() {
        when(entityManager.find(
                Grupo.class,
                GRUPO_ID,
                LockModeType.PESSIMISTIC_WRITE
        )).thenReturn(null);

        assertThrows(
                ResponseStatusException.class,
                () -> service.matricular(
                        new MatriculaRequest(
                                ESTUDIANTE_ID,
                                GRUPO_ID
                        )
                )
        );
    }

    @Test
    void rechazaEstudianteInactivo() {
        prepararGrupoAbierto();

        when(estudiantes.findById(ESTUDIANTE_ID))
                .thenReturn(Optional.of(estudiante));

        when(estudiante.getUsuario()).thenReturn(usuario);
        when(usuario.isActivo()).thenReturn(false);

        assertThrows(
                ResponseStatusException.class,
                () -> service.matricular(
                        new MatriculaRequest(
                                ESTUDIANTE_ID,
                                GRUPO_ID
                        )
                )
        );

        verify(matriculas, never()).saveAndFlush(any());
    }

    @Test
    void rechazaMatriculaDuplicada() {
        prepararGrupoAbierto();
        prepararEstudianteActivo();

        Matricula existente = new Matricula(estudiante, grupo);

        when(matriculas.findByEstudianteIdAndGrupoId(
                ESTUDIANTE_ID,
                GRUPO_ID
        )).thenReturn(Optional.of(existente));

        assertThrows(
                ResponseStatusException.class,
                () -> service.matricular(
                        new MatriculaRequest(
                                ESTUDIANTE_ID,
                                GRUPO_ID
                        )
                )
        );

        verify(matriculas, never()).saveAndFlush(any());
    }

    @Test
    void rechazaGrupoSinCupos() {
        prepararGrupoAbierto();
        prepararEstudianteActivo();

        when(matriculas.findByEstudianteIdAndGrupoId(
                ESTUDIANTE_ID,
                GRUPO_ID
        )).thenReturn(Optional.empty());

        when(grupo.getCupo()).thenReturn(30);

        when(matriculas.contarActivasPorGrupo(GRUPO_ID))
                .thenReturn(30L);

        assertThrows(
                ResponseStatusException.class,
                () -> service.matricular(
                        new MatriculaRequest(
                                ESTUDIANTE_ID,
                                GRUPO_ID
                        )
                )
        );

        verify(matriculas, never()).saveAndFlush(any());
    }

    @Test
    void permiteMatriculaConCupoDisponible() {
        prepararGrupoAbierto();
        prepararEstudianteActivo();

        when(matriculas.findByEstudianteIdAndGrupoId(
                ESTUDIANTE_ID,
                GRUPO_ID
        )).thenReturn(Optional.empty());

        when(grupo.getCupo()).thenReturn(30);

        when(matriculas.contarActivasPorGrupo(GRUPO_ID))
                .thenReturn(29L);

        when(usuario.getNombres()).thenReturn("Ana");
        when(usuario.getApellidos()).thenReturn("Pérez");
        when(estudiante.getCodigo()).thenReturn("EST-001");
        when(grupo.getCodigo()).thenReturn("PROG101-G01");

        when(matriculas.saveAndFlush(any(Matricula.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        MatriculaResponse resultado = service.matricular(
                new MatriculaRequest(
                        ESTUDIANTE_ID,
                        GRUPO_ID
                )
        );

        assertEquals("ACTIVA", resultado.estado());
        assertEquals(ESTUDIANTE_ID, resultado.estudianteId());
        assertEquals(GRUPO_ID, resultado.grupoId());

        verify(matriculas).saveAndFlush(any(Matricula.class));
    }

    @Test
    void permiteReactivarMatriculaRetirada() {
        prepararGrupoAbierto();
        prepararEstudianteActivo();

        Matricula existente = new Matricula(estudiante, grupo);
        existente.retirar();

        when(matriculas.findByEstudianteIdAndGrupoId(
                ESTUDIANTE_ID,
                GRUPO_ID
        )).thenReturn(Optional.of(existente));

        when(grupo.getCupo()).thenReturn(30);

        when(matriculas.contarActivasPorGrupo(GRUPO_ID))
                .thenReturn(29L);

        when(usuario.getNombres()).thenReturn("Ana");
        when(usuario.getApellidos()).thenReturn("Pérez");
        when(estudiante.getCodigo()).thenReturn("EST-001");
        when(grupo.getCodigo()).thenReturn("PROG101-G01");

        MatriculaResponse resultado = service.matricular(
                new MatriculaRequest(
                        ESTUDIANTE_ID,
                        GRUPO_ID
                )
        );

        assertEquals("ACTIVA", resultado.estado());

        verify(matriculas, never()).saveAndFlush(any());
        verify(entityManager).flush();
    }

    @Test
    void permiteRetirarMatriculaActiva() {
        Matricula matricula = new Matricula(
                estudiante,
                grupo
        );

        when(matriculas.findById(5L))
                .thenReturn(Optional.of(matricula));

        when(grupo.getId()).thenReturn(GRUPO_ID);

        when(entityManager.find(
                Grupo.class,
                GRUPO_ID,
                LockModeType.PESSIMISTIC_WRITE
        )).thenReturn(grupo);

        when(estudiante.getUsuario()).thenReturn(usuario);
        when(usuario.getNombres()).thenReturn("Ana");
        when(usuario.getApellidos()).thenReturn("Pérez");
        when(estudiante.getCodigo()).thenReturn("EST-001");
        when(grupo.getCodigo()).thenReturn("PROG101-G01");

        MatriculaResponse resultado = service.retirar(5L);

        assertEquals("RETIRADA", resultado.estado());

        verify(entityManager).flush();
    }
}
