package com.unigest.calificacion;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;

/** Cálculo de un promedio definitivo sólo para evaluaciones completas y publicables. */
public final class CalculadoraNotas {
    private static final BigDecimal CIEN = new BigDecimal("100.00");
    private static final BigDecimal MAX = new BigDecimal("5.00");
    private CalculadoraNotas() { }

    public record EvaluacionCalificada(BigDecimal nota, BigDecimal porcentaje) { }

    public static BigDecimal calcularDefinitiva(Collection<EvaluacionCalificada> evaluaciones) {
        if (evaluaciones == null || evaluaciones.isEmpty()) {
            throw new IllegalArgumentException("Debe haber al menos una evaluacion");
        }
        BigDecimal porcentajeTotal = BigDecimal.ZERO;
        BigDecimal sumaPonderada = BigDecimal.ZERO;
        for (EvaluacionCalificada evaluacion : evaluaciones) {
            if (evaluacion == null || evaluacion.nota() == null || evaluacion.porcentaje() == null) {
                throw new IllegalArgumentException("Hay una evaluacion sin nota o ponderacion");
            }
            if (evaluacion.nota().compareTo(BigDecimal.ZERO) < 0 || evaluacion.nota().compareTo(MAX) > 0) {
                throw new IllegalArgumentException("La nota debe estar entre 0 y 5");
            }
            if (evaluacion.porcentaje().compareTo(BigDecimal.ZERO) <= 0
                    || evaluacion.porcentaje().compareTo(CIEN) > 0) {
                throw new IllegalArgumentException("Ponderacion invalida");
            }
            porcentajeTotal = porcentajeTotal.add(evaluacion.porcentaje());
            sumaPonderada = sumaPonderada.add(evaluacion.nota().multiply(evaluacion.porcentaje()));
        }
        if (porcentajeTotal.compareTo(CIEN) != 0) {
            throw new IllegalArgumentException("Las ponderaciones deben sumar 100%");
        }
        return sumaPonderada.divide(CIEN, 2, RoundingMode.HALF_UP);
    }

    public static boolean aprobo(BigDecimal definitiva, BigDecimal minima) {
        if (definitiva == null || minima == null) throw new IllegalArgumentException("Notas requeridas");
        return definitiva.compareTo(minima) >= 0;
    }
}
