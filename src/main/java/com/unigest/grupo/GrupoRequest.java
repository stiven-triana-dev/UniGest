package com.unigest.grupo;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record GrupoRequest(

    @NotNull(message = "La asignatura es obligatoria")
    Long asignaturaId,

    @NotNull(message = "El docente es obligatorio")
    Long docenteId,

    @NotNull(message = "El periodo académico es obligatorio")
    Long periodoId,

    @NotBlank(message = "El código es obligatorio")
    @Size(max = 15, message = "El código no puede superar 15 caracteres")
    String codigo,

    @NotNull(message = "El cupo es obligatorio")
    @Min(value = 1, message = "El cupo mínimo es 1")
    @Max(value = 1000, message = "El cupo máximo es 1000")
    Integer cupo

) {
}
