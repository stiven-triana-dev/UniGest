package com.unigest.asignatura;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AsignaturaRepository extends JpaRepository<Asignatura, Long> {
    List<Asignatura> findByActivaTrueOrderByNombreAsc();
}
