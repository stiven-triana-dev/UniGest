package com.unigest.demo;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

/** Carga los datos de la demo dentro de una transacción que se revierte al final. */
@SpringBootTest
@Transactional
@Rollback
class DemoInicializadorIntegracionTest {

    @Autowired private DemoInicializador inicializador;
    @Autowired private DemoModo demo;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private PasswordEncoder encoder;

    private int contar(String sql) {
        return jdbc.queryForObject(sql, Integer.class);
    }

    @Test
    void cargaUnaDemoCompletaYCoherente() {
        inicializador.reiniciar();

        assertEquals(13, contar("SELECT COUNT(*)::int FROM usuarios"));
        assertEquals(3, contar("SELECT COUNT(*)::int FROM docentes"));
        assertEquals(9, contar("SELECT COUNT(*)::int FROM estudiantes"));
        assertEquals(7, contar("SELECT COUNT(*)::int FROM grupos"));
        assertEquals(1, contar("SELECT COUNT(*)::int FROM periodos WHERE estado = 'EN_CURSO'"));
        assertEquals(1, contar("SELECT COUNT(*)::int FROM historial_calificaciones"));

        // En cada grupo con evaluaciones, los porcentajes suman 100 (regla para publicar).
        List<BigDecimal> sumas = jdbc.queryForList(
                "SELECT SUM(ponderacion) FROM evaluaciones GROUP BY grupo_id", BigDecimal.class);
        assertEquals(6, sumas.size());
        sumas.forEach(suma -> assertEquals(0, suma.compareTo(new BigDecimal("100.00"))));

        // Toda evaluación publicada tiene nota para cada matrícula activa o finalizada.
        assertEquals(0, contar("""
                SELECT COUNT(*)::int FROM evaluaciones e
                JOIN matriculas m ON m.grupo_id = e.grupo_id AND m.estado IN ('ACTIVA','FINALIZADA')
                LEFT JOIN calificaciones c ON c.evaluacion_id = e.id AND c.matricula_id = m.id
                WHERE e.publicada AND c.id IS NULL"""));
        assertEquals(0, contar("SELECT COUNT(*)::int FROM calificaciones WHERE nota < 0 OR nota > 5"));
    }

    @Test
    void soloLasTresCuentasDePruebaPuedenEntrar() {
        inicializador.reiniciar();

        List<String> conRol = jdbc.queryForList("""
                SELECT u.correo || ':' || r.codigo FROM usuarios u
                JOIN usuario_roles ur ON ur.usuario_id = u.id JOIN roles r ON r.id = ur.rol_id
                WHERE u.correo IN ('admin@demo.test','docente@demo.test','estudiante@demo.test')
                ORDER BY u.correo""", String.class);
        assertEquals(List.of("admin@demo.test:ADMIN", "docente@demo.test:DOCENTE",
                "estudiante@demo.test:ESTUDIANTE"), conRol);

        for (var correo : List.of(DemoModo.CORREO_ADMIN, DemoModo.CORREO_DOCENTE, DemoModo.CORREO_ESTUDIANTE)) {
            String hash = jdbc.queryForObject("SELECT clave_hash FROM usuarios WHERE correo = ?", String.class, correo);
            assertTrue(encoder.matches(demo.getClave(), hash), correo);
        }
        String otra = jdbc.queryForObject("SELECT clave_hash FROM usuarios WHERE correo = 'v.rios@demo.test'", String.class);
        assertFalse(encoder.matches(demo.getClave(), otra), "las demás cuentas no usan la contraseña pública");
        assertFalse(jdbc.queryForList("SELECT clave_hash FROM usuarios", String.class).stream()
                .anyMatch(h -> h.contains("{{")), "no quedan marcadores sin reemplazar");
    }
}
