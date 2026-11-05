package com.unigest.periodo;

import java.time.LocalDate;

public record PeriodoResponse(
        Long id,
        String codigo,
        String nombre,
        LocalDate fechaInicio,
        LocalDate fechaFin,
        String estado
) {
}
