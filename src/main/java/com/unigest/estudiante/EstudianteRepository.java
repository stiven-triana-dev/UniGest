package com.unigest.estudiante;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EstudianteRepository extends JpaRepository<Estudiante, Long> {
    boolean existsByCodigo(String codigo);
    boolean existsByCodigoAndIdNot(String codigo, Long id);
    boolean existsByDocumento(String documento);
    boolean existsByDocumentoAndIdNot(String documento, Long id);

    @Query("""
        select e from Estudiante e join e.usuario u join e.programa p
        where u.activo = true
          and (:programaId is null or p.id = :programaId)
          and (:texto = '' or lower(u.nombres) like concat('%', :texto, '%')
               or lower(u.apellidos) like concat('%', :texto, '%')
               or lower(u.correo) like concat('%', :texto, '%')
               or lower(e.codigo) like concat('%', :texto, '%')
               or lower(e.documento) like concat('%', :texto, '%'))
        """)
    Page<Estudiante> buscarActivos(@Param("texto") String texto, @Param("programaId") Long programaId, Pageable pageable);
}
