package com.unigest.docente;

import java.time.LocalDate;

public record DocenteResponse(
        Long id,
        String codigo,
        String documento,
        String nombres,
        String apellidos,
        String correo,
        LocalDate fechaVinculacion
) {
}
