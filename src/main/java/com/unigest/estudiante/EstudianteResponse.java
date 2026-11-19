package com.unigest.estudiante;

import java.time.LocalDate;

public record EstudianteResponse(
        Long id, String codigo, String documento, String nombres, String apellidos,
        String correo, Long programaId, String programa, LocalDate fechaIngreso
) { }
