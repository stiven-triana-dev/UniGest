package com.unigest.grupo;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GrupoRepository extends JpaRepository<Grupo, Long> {

    @Override
    @EntityGraph(attributePaths = {
        "asignatura",
        "docente",
        "docente.usuario",
        "periodo"
    })
    List<Grupo> findAll();

    @Override
    @EntityGraph(attributePaths = {
        "asignatura",
        "docente",
        "docente.usuario",
        "periodo"
    })
    Optional<Grupo> findById(Long id);

    boolean existsByPeriodoIdAndAsignaturaIdAndCodigo(
        Long periodoId,
        Long asignaturaId,
        String codigo
    );

    boolean existsByPeriodoIdAndAsignaturaIdAndCodigoAndIdNot(
        Long periodoId,
        Long asignaturaId,
        String codigo,
        Long id
    );

    List<Grupo> findByPeriodoId(Long periodoId);

    List<Grupo> findByDocenteId(Long docenteId);
}
