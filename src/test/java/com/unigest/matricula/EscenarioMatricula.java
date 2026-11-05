package com.unigest.matricula;

import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

/** Datos de integración aislados: no dependen de IDs ni registros del usuario. */
final class EscenarioMatricula {
    record Datos(Long grupoId, Long estudianteSinMatriculaId) { }

    private EscenarioMatricula() { }

    static Datos grupoLleno(JdbcTemplate jdbc) {
        String unico = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        Long programaId = idSemilla(jdbc, "SELECT id FROM programas ORDER BY id LIMIT 1");
        Long asignaturaId = idSemilla(jdbc, "SELECT id FROM asignaturas ORDER BY id LIMIT 1");
        Long periodoId = idSemilla(jdbc, "SELECT id FROM periodos ORDER BY id LIMIT 1");

        Long docenteId = nuevoUsuario(jdbc, "doc-" + unico + "@example.test");
        jdbc.update("""
            INSERT INTO docentes (usuario_id, codigo, documento, fecha_vinculacion)
            VALUES (?, ?, ?, CURRENT_DATE)
            """, docenteId, "D-" + unico, "DOC-" + unico);

        Long grupoId = jdbc.queryForObject("""
            INSERT INTO grupos (asignatura_id, docente_id, periodo_id, codigo, cupo, estado)
            VALUES (?, ?, ?, ?, 1, 'ABIERTO') RETURNING id
            """, Long.class, asignaturaId, docenteId, periodoId, "G" + unico);

        Long matriculadoId = nuevoEstudiante(jdbc, programaId, unico + "a");
        Long sinMatriculaId = nuevoEstudiante(jdbc, programaId, unico + "b");

        jdbc.update("""
            INSERT INTO matriculas (estudiante_id, grupo_id, estado)
            VALUES (?, ?, 'ACTIVA')
            """, matriculadoId, grupoId);

        return new Datos(grupoId, sinMatriculaId);
    }

    private static Long idSemilla(JdbcTemplate jdbc, String sql) {
        Long id = jdbc.queryForObject(sql, Long.class);
        if (id == null) throw new IllegalStateException("Falta el catálogo inicial de Flyway");
        return id;
    }

    private static Long nuevoUsuario(JdbcTemplate jdbc, String correo) {
        return jdbc.queryForObject("""
            INSERT INTO usuarios (nombres, apellidos, correo, clave_hash, activo)
            VALUES ('Prueba', 'Integración', ?, 'HASH_FICTICIO_NO_UTILIZABLE_123456', true)
            RETURNING id
            """, Long.class, correo);
    }

    private static Long nuevoEstudiante(JdbcTemplate jdbc, Long programaId, String unico) {
        Long usuarioId = nuevoUsuario(jdbc, "est-" + unico + "@example.test");
        jdbc.update("""
            INSERT INTO estudiantes (usuario_id, codigo, documento, programa_id, fecha_ingreso)
            VALUES (?, ?, ?, ?, CURRENT_DATE)
            """, usuarioId, "EST-" + unico, "IDENT-" + unico, programaId);
        return usuarioId;
    }
}
