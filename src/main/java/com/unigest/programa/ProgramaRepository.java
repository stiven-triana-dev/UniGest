package com.unigest.programa;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProgramaRepository extends JpaRepository<Programa, Long> {
    List<Programa> findByActivoTrueOrderByNombreAsc();
}
