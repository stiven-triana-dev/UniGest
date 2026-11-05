package com.unigest.evaluacion;

import com.unigest.grupo.Grupo;
import com.unigest.grupo.GrupoRepository;
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
class EvaluacionServiceTest {
    @Mock EvaluacionRepository evaluaciones;
    @Mock GrupoRepository grupos;
    @InjectMocks EvaluacionService servicio;

    private Grupo grupo(String estado) {
        Grupo g = mock(Grupo.class);
        when(g.getEstado()).thenReturn(estado);
        return g;
    }

    @Test void noPermiteCrearEnBorrador() {
        Grupo g = grupo("BORRADOR");
        when(grupos.findById(1L)).thenReturn(Optional.of(g));
        var request = new EvaluacionRequest(1L, "Parcial", new BigDecimal("40.00"), null);
        var error = assertThrows(ResponseStatusException.class, () -> servicio.crear(request));
        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        verify(evaluaciones, never()).save(any());
    }

    @Test void noPermitePonderacionSuperiorACien() {
        Grupo g = grupo("ABIERTO");
        when(g.getId()).thenReturn(1L);
        when(grupos.findById(1L)).thenReturn(Optional.of(g));
        Evaluacion previa = mock(Evaluacion.class);
        when(previa.getPonderacion()).thenReturn(new BigDecimal("80.00"));
        when(evaluaciones.findByGrupoIdOrderByIdAsc(1L)).thenReturn(List.of(previa));
        var error = assertThrows(ResponseStatusException.class, () -> servicio.crear(
            new EvaluacionRequest(1L, "Parcial 2", new BigDecimal("30.00"), null)));
        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        verify(evaluaciones, never()).save(any());
    }

    @Test void noPermitePublicarConPonderacionIncompleta() {
        Grupo g = mock(Grupo.class);
        when(g.getId()).thenReturn(1L);
        Evaluacion e = mock(Evaluacion.class);
        when(e.isPublicada()).thenReturn(false);
        when(e.getGrupo()).thenReturn(g);
        when(e.getPonderacion()).thenReturn(new BigDecimal("60.00"));
        when(evaluaciones.findById(2L)).thenReturn(Optional.of(e));
        when(evaluaciones.findByGrupoIdOrderByIdAsc(1L)).thenReturn(List.of(e));
        var error = assertThrows(ResponseStatusException.class, () -> servicio.publicar(2L));
        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        verify(e, never()).publicar();
    }

    @Test void publicaConPonderacionCompleta() {
        Grupo g = mock(Grupo.class);
        when(g.getId()).thenReturn(1L);
        Evaluacion e = mock(Evaluacion.class);
        when(e.isPublicada()).thenReturn(false);
        when(e.getGrupo()).thenReturn(g);
        when(e.getPonderacion()).thenReturn(new BigDecimal("100.00"));
        when(evaluaciones.findById(2L)).thenReturn(Optional.of(e));
        when(evaluaciones.findByGrupoIdOrderByIdAsc(1L)).thenReturn(List.of(e));
        servicio.publicar(2L);
        verify(e).publicar();
    }
}
