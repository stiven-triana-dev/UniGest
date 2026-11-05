package com.unigest.matricula;

import java.time.OffsetDateTime;

public record MatriculaResponse(

    Long id,
    Long estudianteId,
    String estudianteNombre,
    String estudianteCodigo,
    Long grupoId,
    String grupoCodigo,
    OffsetDateTime fecha,
    String estado

) {
    public static MatriculaResponse desde(Matricula matricula) {

        var estudiante = matricula.getEstudiante();
        var usuario = estudiante.getUsuario();
        var grupo = matricula.getGrupo();

        return new MatriculaResponse(
            matricula.getId(),
            estudiante.getId(),
            usuario.getNombres() + " " + usuario.getApellidos(),
            estudiante.getCodigo(),
            grupo.getId(),
            grupo.getCodigo(),
            matricula.getFecha(),
            matricula.getEstado()
        );
    }
}
