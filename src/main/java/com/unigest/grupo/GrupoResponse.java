package com.unigest.grupo;

public record GrupoResponse(

    Long id,

    Long asignaturaId,
    String asignaturaNombre,

    Long docenteId,
    String docenteNombre,

    Long periodoId,
    String periodoNombre,

    String codigo,
    Integer cupo,
    String estado

) {
}
