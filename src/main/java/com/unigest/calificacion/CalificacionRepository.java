package com.unigest.calificacion;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CalificacionRepository extends JpaRepository<Calificacion, Long> {
    @EntityGraph(attributePaths = {"matricula", "evaluacion"})
    List<Calificacion> findByEvaluacionId(Long evaluacionId);
    @EntityGraph(attributePaths = {"matricula", "evaluacion"})
    List<Calificacion> findByGrupoId(Long grupoId);
    Optional<Calificacion> findByMatriculaIdAndEvaluacionId(Long matriculaId, Long evaluacionId);
}
