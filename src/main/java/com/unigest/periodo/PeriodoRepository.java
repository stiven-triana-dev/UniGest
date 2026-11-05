package com.unigest.periodo;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PeriodoRepository
        extends JpaRepository<Periodo, Long> {

    Optional<Periodo> findByCodigoIgnoreCase(String codigo);

    boolean existsByCodigoIgnoreCase(String codigo);

    List<Periodo> findByEstadoOrderByFechaInicioDesc(String estado);

    List<Periodo> findAllByOrderByFechaInicioDesc();
}
