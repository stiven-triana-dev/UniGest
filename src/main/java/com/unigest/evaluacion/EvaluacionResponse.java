package com.unigest.evaluacion;

import java.math.BigDecimal;
import java.time.LocalDate;

public record EvaluacionResponse(Long id, Long grupoId, String titulo,
        BigDecimal ponderacion, LocalDate fecha, boolean publicada) {
    public static EvaluacionResponse desde(Evaluacion e) {
        return new EvaluacionResponse(e.getId(), e.getGrupo().getId(),
            e.getTitulo(), e.getPonderacion(), e.getFecha(), e.isPublicada());
    }
}
