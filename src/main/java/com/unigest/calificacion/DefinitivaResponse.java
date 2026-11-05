package com.unigest.calificacion;

import java.math.BigDecimal;

public record DefinitivaResponse(Long matriculaId, String estudianteNombre,
    String estudianteCodigo, BigDecimal definitiva, Boolean aprobado) {}
