package com.unigest.estudiante;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

public record EstudianteRequest(
        @NotBlank @Size(max = 100) String nombres,
        @NotBlank @Size(max = 100) String apellidos,
        @NotBlank @Email @Size(max = 180) String correo,
        @NotBlank @Size(max = 30) String documento,
        @NotBlank @Size(max = 30) String codigo,
        @NotNull @Positive Long programaId,
        @PastOrPresent LocalDate fechaIngreso
) { }
