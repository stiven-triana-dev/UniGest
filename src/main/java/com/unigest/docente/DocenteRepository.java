package com.unigest.docente;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DocenteRepository extends JpaRepository<Docente, Long> {

    boolean existsByCodigo(String codigo);

    boolean existsByDocumento(String documento);
}
