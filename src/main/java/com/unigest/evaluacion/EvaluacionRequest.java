package com.unigest.evaluacion;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public record EvaluacionRequest(
    @NotNull @Positive Long grupoId,
    @NotBlank @Size(max = 150) String titulo,
    @NotNull @DecimalMin("0.01") @DecimalMax("100.00") BigDecimal ponderacion,
    LocalDate fecha
) {}
