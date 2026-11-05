package com.unigest.usuario;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository
        extends JpaRepository<Usuario, Long> {

    boolean existsByCorreoIgnoreCase(String correo);

    boolean existsByCorreoIgnoreCaseAndIdNot(
            String correo, Long id
    );

    @EntityGraph(attributePaths = "roles")
    Optional<Usuario> findByCorreoIgnoreCase(String correo);
}