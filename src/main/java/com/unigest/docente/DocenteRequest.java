package com.unigest.docente;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record DocenteRequest(

        @NotBlank
        @Size(max = 100)
        String nombres,

        @NotBlank
        @Size(max = 100)
        String apellidos,

        @NotBlank
        @Email
        @Size(max = 180)
        String correo,

        @NotBlank
        @Size(max = 30)
        String codigo,

        @NotBlank
        @Size(max = 30)
        String documento,

        @PastOrPresent
        LocalDate fechaVinculacion
) {
}
