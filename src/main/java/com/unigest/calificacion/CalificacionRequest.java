package com.unigest.calificacion;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record CalificacionRequest(
    @NotNull @Positive Long matriculaId,
    @NotNull @Positive Long evaluacionId,
    @NotNull @DecimalMin("0.00") @DecimalMax("5.00") BigDecimal nota,
    @Size(max = 255) String motivo
) {}
