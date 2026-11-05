package com.unigest.calificacion;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CalculadoraNotasTest {
    private CalculadoraNotas.EvaluacionCalificada e(String nota, String porcentaje) {
        return new CalculadoraNotas.EvaluacionCalificada(new BigDecimal(nota), new BigDecimal(porcentaje));
    }

    @Test void ponderaCorrectamente() {
        var resultado = CalculadoraNotas.calcularDefinitiva(List.of(e("4.50", "40"), e("3.50", "60")));
        assertEquals(new BigDecimal("3.90"), resultado);
    }
    @Test void exigeCienPorCiento() {
        assertThrows(IllegalArgumentException.class,
                () -> CalculadoraNotas.calcularDefinitiva(List.of(e("4", "70"))));
    }
    @Test void rechazaNotasFueraDeEscala() {
        assertThrows(IllegalArgumentException.class,
                () -> CalculadoraNotas.calcularDefinitiva(List.of(e("5.10", "100"))));
    }
    @Test void verificaUmbral() {
        assertTrue(CalculadoraNotas.aprobo(new BigDecimal("3.00"), new BigDecimal("3.00")));
        assertFalse(CalculadoraNotas.aprobo(new BigDecimal("2.99"), new BigDecimal("3.00")));
    }
}
