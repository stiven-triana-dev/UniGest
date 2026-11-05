package com.unigest.calificacion;

import java.math.BigDecimal;

public record NotaEstudianteResponse(Long matriculaId, String estudianteNombre,
        String estudianteCodigo, String estado, Long calificacionId,
        BigDecimal nota) {}
