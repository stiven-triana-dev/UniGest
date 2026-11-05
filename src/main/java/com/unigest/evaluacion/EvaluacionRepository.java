package com.unigest.evaluacion;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import java.util.List;
import java.util.Optional;

public interface EvaluacionRepository extends JpaRepository<Evaluacion, Long> {
    @EntityGraph(attributePaths = "grupo")
    List<Evaluacion> findByGrupoIdOrderByIdAsc(Long grupoId);
    @Override
    @EntityGraph(attributePaths = "grupo")
    Optional<Evaluacion> findById(Long id);
}
