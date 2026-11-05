package com.unigest.reporte;

import com.unigest.calificacion.Calificacion;
import com.unigest.calificacion.CalificacionRepository;
import com.unigest.estudiante.Estudiante;
import com.unigest.estudiante.EstudianteRepository;
import com.unigest.evaluacion.Evaluacion;
import com.unigest.evaluacion.EvaluacionRepository;
import com.unigest.grupo.Grupo;
import com.unigest.grupo.GrupoRepository;
import com.unigest.matricula.Matricula;
import com.unigest.matricula.MatriculaRepository;
import com.unigest.periodo.Periodo;
import com.unigest.periodo.PeriodoRepository;

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
class ReporteAcademicoServiceTest {

    @Mock
    GrupoRepository grupos;

    @Mock
    PeriodoRepository periodos;

    @Mock
    EstudianteRepository estudiantes;

    @Mock
    MatriculaRepository matriculas;

    @Mock
    EvaluacionRepository evaluaciones;

    @Mock
    CalificacionRepository calificaciones;

    @InjectMocks
    ReporteAcademicoService servicio;

    // DATOS SIMULADOS PARA LAS PRUEBAS

    private Grupo grupo(Long id) {
        Grupo g = mock(Grupo.class, RETURNS_DEEP_STUBS);

        when(g.getId()).thenReturn(id);
        when(g.getCodigo()).thenReturn("PROG101-G01");

        when(g.getAsignatura().getCodigo())
            .thenReturn("PROG101");

        when(g.getAsignatura().getNombre())
            .thenReturn("Fundamentos de Programación");

        when(g.getPeriodo().getId())
            .thenReturn(2L);

        when(g.getPeriodo().getNombre())
            .thenReturn("2026-2");

        when(g.getDocente().getUsuario().getNombres())
            .thenReturn("Ana");

        when(g.getDocente().getUsuario().getApellidos())
            .thenReturn("Prueba");

        return g;
    }

    private Estudiante estudiante() {
        Estudiante e = mock(Estudiante.class, RETURNS_DEEP_STUBS);

        when(e.getId()).thenReturn(5L);
        when(e.getCodigo()).thenReturn("EST-MAT-001");

        when(e.getUsuario().getNombres())
            .thenReturn("Carlos");

        when(e.getUsuario().getApellidos())
            .thenReturn("Andrés");

        return e;
    }

    private Matricula matricula(String estado, Estudiante e) {
        Matricula m = mock(Matricula.class);

        when(m.getEstado()).thenReturn(estado);

        if ("ACTIVA".equals(estado)) {
            when(m.getId()).thenReturn(9L);
            when(m.getEstudiante()).thenReturn(e);
        }

        return m;
    }

    private Evaluacion evaluacion(
        Long id,
        String titulo,
        String peso,
        boolean publicada
    ) {
        Evaluacion e = mock(Evaluacion.class);

        when(e.getId()).thenReturn(id);
        when(e.getTitulo()).thenReturn(titulo);

        when(e.getPonderacion())
            .thenReturn(new BigDecimal(peso));

        when(e.isPublicada()).thenReturn(publicada);

        return e;
    }

    private Calificacion nota(Long evalId, String valor) {
        Calificacion c = mock(
            Calificacion.class,
            RETURNS_DEEP_STUBS
        );

        when(c.getMatricula().getId())
            .thenReturn(9L);

        when(c.getEvaluacion().getId())
            .thenReturn(evalId);

        when(c.getNota())
            .thenReturn(new BigDecimal(valor));

        return c;
    }

    // PRUEBA 1: CÁLCULO DE LA DEFINITIVA

    @Test
    void reporteGrupoCalculaDefinitiva474YEstadisticas() {

        // Primero crear todos los datos simulados.
        Grupo g = grupo(1L);

        Estudiante e = estudiante();

        Matricula m = matricula("ACTIVA", e);

        List<Evaluacion> actividades = List.of(
            evaluacion(
                11L,
                "Parcial 1",
                "30.00",
                true
            ),
            evaluacion(
                12L,
                "Parcial 2",
                "30.00",
                true
            ),
            evaluacion(
                13L,
                "Proyecto final",
                "40.00",
                true
            )
        );

        List<Calificacion> notas = List.of(
            nota(11L, "4.70"),
            nota(12L, "4.70"),
            nota(13L, "4.80")
        );

        // Después configurar los repositorios.
        when(grupos.findById(1L))
            .thenReturn(Optional.of(g));

        when(matriculas.findByGrupoIdOrderByIdAsc(1L))
            .thenReturn(List.of(m));

        when(evaluaciones.findByGrupoIdOrderByIdAsc(1L))
            .thenReturn(actividades);

        when(calificaciones.findByGrupoId(1L))
            .thenReturn(notas);

        // Ejecutar el servicio.
        var r = servicio.reporteGrupo(1L);

        // Comprobar resultados.
        assertEquals(
            new BigDecimal("4.74"),
            r.estudiantes().getFirst().definitiva()
        );

        assertTrue(
            r.estudiantes().getFirst().aprobado()
        );

        assertEquals(3, r.evaluaciones().size());
        assertEquals(1, r.resumen().completas());
        assertEquals(1, r.resumen().aprobadas());
        assertEquals(0, r.resumen().pendientes());

        assertEquals(
            new BigDecimal("4.74"),
            r.resumen().promedio()
        );
    }

    // PRUEBA 2: NOTAS INCOMPLETAS

    @Test
    void notaIncompletaSeReportaPendienteSinInventarla() {

        Grupo g = grupo(1L);

        Estudiante e = estudiante();

        Matricula m = matricula("ACTIVA", e);

        Evaluacion actividad = evaluacion(
            11L,
            "Parcial",
            "100.00",
            true
        );

        when(grupos.findById(1L))
            .thenReturn(Optional.of(g));

        when(matriculas.findByGrupoIdOrderByIdAsc(1L))
            .thenReturn(List.of(m));

        when(evaluaciones.findByGrupoIdOrderByIdAsc(1L))
            .thenReturn(List.of(actividad));

        when(calificaciones.findByGrupoId(1L))
            .thenReturn(List.of());

        var r = servicio.reporteGrupo(1L);

        assertNull(
            r.estudiantes().getFirst().definitiva()
        );

        assertNull(
            r.estudiantes().getFirst().aprobado()
        );

        assertEquals(
            1,
            r.resumen().pendientes()
        );

        assertNull(
            r.resumen().promedio()
        );
    }

    // PRUEBA 3: EVALUACIÓN EN BORRADOR

    @Test
    void evaluacionEnBorradorBloqueaDefinitivaAunqueTengaNota() {

        Grupo g = grupo(1L);

        Estudiante e = estudiante();

        Matricula m = matricula("ACTIVA", e);

        Evaluacion actividad = evaluacion(
            11L,
            "Parcial",
            "100.00",
            false
        );

        Calificacion calificacion = nota(
            11L,
            "5.00"
        );

        when(grupos.findById(1L))
            .thenReturn(Optional.of(g));

        when(matriculas.findByGrupoIdOrderByIdAsc(1L))
            .thenReturn(List.of(m));

        when(evaluaciones.findByGrupoIdOrderByIdAsc(1L))
            .thenReturn(List.of(actividad));

        when(calificaciones.findByGrupoId(1L))
            .thenReturn(List.of(calificacion));

        assertNull(
            servicio.reporteGrupo(1L)
                .estudiantes()
                .getFirst()
                .definitiva()
        );
    }

    // PRUEBA 4: MATRÍCULA RETIRADA

    @Test
    void matriculaRetiradaNoApareceEnReporteDeGrupo() {

        Grupo g = grupo(1L);

        Matricula retirada = matricula(
            "RETIRADA",
            null
        );

        when(grupos.findById(1L))
            .thenReturn(Optional.of(g));

        when(matriculas.findByGrupoIdOrderByIdAsc(1L))
            .thenReturn(List.of(retirada));

        when(evaluaciones.findByGrupoIdOrderByIdAsc(1L))
            .thenReturn(List.of());

        when(calificaciones.findByGrupoId(1L))
            .thenReturn(List.of());

        var r = servicio.reporteGrupo(1L);

        assertTrue(
            r.estudiantes().isEmpty()
        );

        assertEquals(
            0,
            r.resumen().total()
        );
    }

    // PRUEBA 5: ESTUDIANTES DEL PERÍODO

    @Test
    void estudiantesDelPeriodoSonUnicosYOmitenRetiros() {

        Periodo p = mock(Periodo.class);

        when(periodos.findById(2L))
            .thenReturn(Optional.of(p));

        Estudiante e = estudiante();

        Matricula primera = mock(Matricula.class);
        Matricula segunda = mock(Matricula.class);
        Matricula retirada = mock(Matricula.class);

        when(primera.getEstado())
            .thenReturn("ACTIVA");

        when(segunda.getEstado())
            .thenReturn("FINALIZADA");

        when(retirada.getEstado())
            .thenReturn("RETIRADA");

        when(primera.getEstudiante())
            .thenReturn(e);

        when(segunda.getEstudiante())
            .thenReturn(e);

        when(matriculas.findByGrupoPeriodoIdOrderByIdAsc(2L))
            .thenReturn(List.of(
                primera,
                segunda,
                retirada
            ));

        var lista = servicio.estudiantesPeriodo(2L);

        assertEquals(
            1,
            lista.size()
        );

        assertEquals(
            "EST-MAT-001",
            lista.getFirst().codigo()
        );
    }

    // PRUEBA 6: BOLETÍN SIN MATERIAS

    @Test
    void boletinSinMateriasDevuelveResumenVacio() {

        Estudiante e = estudiante();

        when(e.getPrograma().getNombre())
            .thenReturn("Ingeniería de Sistemas");

        when(estudiantes.findById(5L))
            .thenReturn(Optional.of(e));

        Periodo p = mock(Periodo.class);

        when(p.getId()).thenReturn(2L);
        when(p.getNombre()).thenReturn("2026-2");

        when(periodos.findById(2L))
            .thenReturn(Optional.of(p));

        when(matriculas.findByEstudianteIdOrderByIdAsc(5L))
            .thenReturn(List.of());

        var r = servicio.boletin(5L, 2L);

        assertTrue(
            r.materias().isEmpty()
        );

        assertEquals(
            0,
            r.resumen().total()
        );

        assertNull(
            r.resumen().promedio()
        );
    }

    // PRUEBA 7: PERÍODO INEXISTENTE

    @Test
    void periodoInexistenteResponde404() {

        when(periodos.findById(999L))
            .thenReturn(Optional.empty());

        var ex = assertThrows(
            ResponseStatusException.class,
            () -> servicio.estudiantesPeriodo(999L)
        );

        assertEquals(
            HttpStatus.NOT_FOUND,
            ex.getStatusCode()
        );
    }

    // PRUEBA 8: GRUPO INEXISTENTE

    @Test
    void grupoInexistenteResponde404() {

        when(grupos.findById(999L))
            .thenReturn(Optional.empty());

        var ex = assertThrows(
            ResponseStatusException.class,
            () -> servicio.reporteGrupo(999L)
        );

        assertEquals(
            HttpStatus.NOT_FOUND,
            ex.getStatusCode()
        );
    }
}
