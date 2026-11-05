package com.unigest.calificacion;

import com.unigest.evaluacion.*;
import com.unigest.grupo.*;
import com.unigest.matricula.*;
import com.unigest.estudiante.Estudiante;
import com.unigest.usuario.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CalificacionServiceTest {
    @Mock CalificacionRepository notas;
    @Mock HistorialCalificacionRepository historial;
    @Mock EvaluacionRepository evaluaciones;
    @Mock MatriculaRepository matriculas;
    @Mock GrupoRepository grupos;
    @Mock UsuarioRepository usuarios;
    @InjectMocks CalificacionService servicio;

    private void preparar(Long evalGrupo, Long matriculaGrupo, String estado) {
        Grupo ge = mock(Grupo.class), gm = mock(Grupo.class);
        when(ge.getId()).thenReturn(evalGrupo);
        when(gm.getId()).thenReturn(matriculaGrupo);
        Evaluacion e = mock(Evaluacion.class);
        when(e.getGrupo()).thenReturn(ge);
        Matricula m = mock(Matricula.class);
        when(m.getGrupo()).thenReturn(gm);
        when(evaluaciones.findById(5L)).thenReturn(Optional.of(e));
        when(matriculas.findById(9L)).thenReturn(Optional.of(m));
        if (evalGrupo.equals(matriculaGrupo)) when(m.getEstado()).thenReturn(estado);
    }

    @Test void rechazaMatriculaDeOtroGrupo() {
        preparar(1L,2L,"ACTIVA");
        var error = assertThrows(ResponseStatusException.class, () -> servicio.guardar(
            new CalificacionRequest(9L,5L,new BigDecimal("4.00"),null),"admin@unigest.local"));
        assertEquals(HttpStatus.CONFLICT,error.getStatusCode());
        verify(notas,never()).save(any());
    }

    @Test void rechazaMatriculaRetirada() {
        preparar(1L,1L,"RETIRADA");
        var error = assertThrows(ResponseStatusException.class, () -> servicio.guardar(
            new CalificacionRequest(9L,5L,new BigDecimal("4.00"),null),"admin@unigest.local"));
        assertEquals(HttpStatus.CONFLICT,error.getStatusCode());
    }

    @Test void exigeMotivoParaCorregirNota() {
        preparar(1L,1L,"ACTIVA");
        Calificacion existente = mock(Calificacion.class);
        when(existente.getNota()).thenReturn(new BigDecimal("3.00"));
        when(notas.findByMatriculaIdAndEvaluacionId(9L,5L)).thenReturn(Optional.of(existente));
        var error = assertThrows(ResponseStatusException.class, () -> servicio.guardar(
            new CalificacionRequest(9L,5L,new BigDecimal("4.00"),"  "),"admin@unigest.local"));
        assertEquals(HttpStatus.BAD_REQUEST,error.getStatusCode());
        verify(historial,never()).save(any());
    }

    @Test void rechazaMasDeDosDecimales() {
        preparar(1L,1L,"ACTIVA");
        var error = assertThrows(ResponseStatusException.class, () -> servicio.guardar(
            new CalificacionRequest(9L,5L,new BigDecimal("3.123"),null),"admin@unigest.local"));
        assertEquals(HttpStatus.BAD_REQUEST,error.getStatusCode());
    }
}
