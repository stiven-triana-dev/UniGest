
package com.unigest.matricula;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MatriculaRepository
        extends JpaRepository<Matricula, Long> {

    List<Matricula> findByGrupoIdOrderByIdAsc(Long grupoId);

    List<Matricula> findByEstudianteIdOrderByIdAsc(Long estudianteId);

    // Etapa 9: estudiantes con matrícula en un período académico.
    List<Matricula> findByGrupoPeriodoIdOrderByIdAsc(Long periodoId);

    Optional<Matricula> findByEstudianteIdAndGrupoId(
            Long estudianteId,
            Long grupoId
    );

    @Query("""
        SELECT COUNT(m)
        FROM Matricula m
        WHERE m.grupo.id = :grupoId
          AND m.estado = 'ACTIVA'
        """)
    long contarActivasPorGrupo(
            @Param("grupoId") Long grupoId
    );
}
