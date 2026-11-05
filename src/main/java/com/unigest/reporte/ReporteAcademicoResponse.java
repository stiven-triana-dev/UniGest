package com.unigest.reporte;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** DTOs de solo lectura: nunca exponen correos, documentos o credenciales. */
public final class ReporteAcademicoResponse {
    private ReporteAcademicoResponse() { }

    public record EvaluacionDetalle(Long id, String titulo, BigDecimal ponderacion,
                                     LocalDate fecha, boolean publicada) { }

    public record NotaDetalle(Long evaluacionId, String titulo, BigDecimal ponderacion,
                              BigDecimal nota) { }

    public record Resumen(int total, int completas, int aprobadas, int reprobadas,
                          int pendientes, BigDecimal promedio) { }

    public record FilaGrupo(Long matriculaId, Long estudianteId,
                            String estudianteCodigo, String estudianteNombre,
                            String estado, List<NotaDetalle> notas,
                            BigDecimal definitiva, Boolean aprobado) { }

    public record Grupo(Long grupoId, String grupoCodigo,
                        String asignaturaCodigo, String asignaturaNombre,
                        Long periodoId, String periodoNombre,
                        String docenteNombre, List<EvaluacionDetalle> evaluaciones,
                        List<FilaGrupo> estudiantes, Resumen resumen) { }

    public record EstudianteDisponible(Long id, String codigo, String nombre) { }

    public record Materia(String codigo, String nombre, int creditos,
                          String grupoCodigo, String estadoMatricula,
                          List<NotaDetalle> notas, BigDecimal definitiva,
                          Boolean aprobado) { }

    public record Boletin(Long estudianteId, String estudianteCodigo,
                          String estudianteNombre, String programa,
                          Long periodoId, String periodoNombre,
                          List<Materia> materias, Resumen resumen) { }
}
