package com.unigest.matricula;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record MatriculaRequest(

    @NotNull(message = "El estudiante es obligatorio")
    @Positive(message = "El ID del estudiante debe ser positivo")
    Long estudianteId,

    @NotNull(message = "El grupo es obligatorio")
    @Positive(message = "El ID del grupo debe ser positivo")
    Long grupoId

) {
}
