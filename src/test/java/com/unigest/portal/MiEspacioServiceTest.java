package com.unigest.portal;

import com.unigest.calificacion.CalificacionService;
import com.unigest.estudiante.Estudiante;
import com.unigest.estudiante.EstudianteRepository;
import com.unigest.evaluacion.Evaluacion;
import com.unigest.evaluacion.EvaluacionRepository;
import com.unigest.evaluacion.EvaluacionService;
import com.unigest.grupo.Grupo;
import com.unigest.grupo.GrupoRepository;
import com.unigest.docente.Docente;
import com.unigest.matricula.Matricula;
import com.unigest.matricula.MatriculaRepository;
import com.unigest.periodo.Periodo;
import com.unigest.asignatura.Asignatura;
import com.unigest.reporte.ReporteAcademicoResponse;
import com.unigest.reporte.ReporteAcademicoService;
import com.unigest.usuario.Usuario;
import com.unigest.usuario.UsuarioRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MiEspacioServiceTest {

    @Mock
    UsuarioRepository usuarios;

    @Mock
    EstudianteRepository estudiantes;

    @Mock
    GrupoRepository grupos;

    @Mock
    MatriculaRepository matriculas;

    @Mock
    EvaluacionRepository evaluaciones;

    @Mock
    EvaluacionService gestionEvaluaciones;

    @Mock
    CalificacionService gestionCalificaciones;

    @Mock
    ReporteAcademicoService reportes;

    @InjectMocks
    MiEspacioService servicio;

    private Usuario usuario(Long id, String correo) {
        Usuario u = mock(Usuario.class);

        when(usuarios.findByCorreoIgnoreCase(correo))
            .thenReturn(Optional.of(u));

        when(u.isActivo()).thenReturn(true);
        when(u.getId()).thenReturn(id);

        return u;
    }

    @Test
    void docenteNoPuedeConsultarGrupoDeOtroDocente() {
        usuario(7L, "docente@test.local");

        Grupo g = mock(Grupo.class);
        Docente otro = mock(Docente.class);

        when(grupos.findById(10L))
            .thenReturn(Optional.of(g));

        when(g.getDocente()).thenReturn(otro);
        when(otro.getId()).thenReturn(8L);

        ResponseStatusException e = assertThrows(
            ResponseStatusException.class,
            () -> servicio.reporteMiGrupo(
                "docente@test.local", 10L
            )
        );

        assertEquals(
            HttpStatus.NOT_FOUND,
            e.getStatusCode()
        );

        verifyNoInteractions(reportes);
    }

    @Test
    void docenteConsultaSoloGrupoAsignado() {
        usuario(7L, "docente@test.local");

        Grupo g = mock(Grupo.class);
        Docente d = mock(Docente.class);

        when(grupos.findById(10L))
            .thenReturn(Optional.of(g));

        when(g.getDocente()).thenReturn(d);
        when(d.getId()).thenReturn(7L);

        when(gestionEvaluaciones.listar(10L))
            .thenReturn(List.of());

        assertTrue(
            servicio.evaluacionesMiGrupo(
                "docente@test.local", 10L
            ).isEmpty()
        );

        verify(gestionEvaluaciones).listar(10L);
    }

    @Test
    void estudianteConsultaPeriodosSegunSuIdentidadNoOtroId() {
        Usuario u = usuario(5L, "alumno@test.local");

        Estudiante e = mock(Estudiante.class);

        when(estudiantes.findById(5L))
            .thenReturn(Optional.of(e));

        when(e.getUsuario()).thenReturn(u);

        // CORRECCIÓN: asignar el ID al estudiante simulado.
        when(e.getId()).thenReturn(5L);

        when(matriculas.findByEstudianteIdOrderByIdAsc(5L))
            .thenReturn(List.of());

        assertTrue(
            servicio.misPeriodos(
                "alumno@test.local"
            ).isEmpty()
        );

        verify(estudiantes).findById(5L);

        verify(estudiantes, never())
            .findById(6L);
    }

    @Test
    void estudianteNoVeNotasDeEvaluacionesEnBorrador() {
        Usuario u = usuario(5L, "alumno@test.local");

        Estudiante e = mock(Estudiante.class);

        when(estudiantes.findById(5L))
            .thenReturn(Optional.of(e));

        when(e.getUsuario()).thenReturn(u);
        when(e.getId()).thenReturn(5L);

        Grupo g = mock(Grupo.class);
        Periodo p = mock(Periodo.class);
        Asignatura a = mock(Asignatura.class);
        Matricula m = mock(Matricula.class);

        when(matriculas.findByEstudianteIdOrderByIdAsc(5L))
            .thenReturn(List.of(m));

        when(m.getEstado()).thenReturn("ACTIVA");
        when(m.getGrupo()).thenReturn(g);

        when(g.getPeriodo()).thenReturn(p);
        when(p.getId()).thenReturn(2L);

        when(g.getId()).thenReturn(10L);
        when(g.getCodigo()).thenReturn("GR-01");

        when(g.getAsignatura()).thenReturn(a);
        when(a.getCodigo()).thenReturn("PROG101");

        Evaluacion publicada = mock(Evaluacion.class);
        Evaluacion borrador = mock(Evaluacion.class);

        when(publicada.isPublicada()).thenReturn(true);
        when(publicada.getId()).thenReturn(11L);

        when(borrador.isPublicada()).thenReturn(false);

        when(evaluaciones.findByGrupoIdOrderByIdAsc(10L))
            .thenReturn(List.of(publicada, borrador));

        var notas = List.of(
            new ReporteAcademicoResponse.NotaDetalle(
                11L,
                "Parcial",
                new BigDecimal("50"),
                new BigDecimal("4.5")
            ),
            new ReporteAcademicoResponse.NotaDetalle(
                12L,
                "Borrador",
                new BigDecimal("50"),
                new BigDecimal("1.0")
            )
        );

        var materia = new ReporteAcademicoResponse.Materia(
            "PROG101",
            "Programación",
            3,
            "GR-01",
            "ACTIVA",
            notas,
            null,
            null
        );

        var resumen = new ReporteAcademicoResponse.Resumen(
            1, 0, 0, 0, 1, null
        );

        var boletin = new ReporteAcademicoResponse.Boletin(
            5L,
            "EST-05",
            "Alumno",
            "Sistemas",
            2L,
            "2026-2",
            List.of(materia),
            resumen
        );

        when(reportes.boletin(5L, 2L))
            .thenReturn(boletin);

        var salida = servicio.miBoletin(
            "alumno@test.local", 2L
        );

        assertEquals(
            1,
            salida.materias()
                .getFirst()
                .notas()
                .size()
        );

        assertEquals(
            11L,
            salida.materias()
                .getFirst()
                .notas()
                .getFirst()
                .evaluacionId()
        );

        assertNull(
            salida.materias()
                .getFirst()
                .definitiva()
        );
    }
}
