package com.unigest.matricula;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
@Rollback
class MatriculaIntegracionTest {
    @Autowired private MatriculaService service;
    @Autowired private JdbcTemplate jdbc;

    @Test
    void rechazaMatriculaCuandoGrupoEstaLleno() {
        var escenario = EscenarioMatricula.grupoLleno(jdbc);
        var solicitud = new MatriculaRequest(
            escenario.estudianteSinMatriculaId(), escenario.grupoId());

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
            () -> service.matricular(solicitud));
        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());

        Integer nuevas = jdbc.queryForObject("""
            SELECT COUNT(*)::integer FROM matriculas
            WHERE estudiante_id = ? AND grupo_id = ?
            """, Integer.class,
            escenario.estudianteSinMatriculaId(), escenario.grupoId());
        assertEquals(0, nuevas);
    }
}
