package com.unigest.grupo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/grupos")
@Validated
public class GrupoController {

    private final GrupoService servicio;

    public GrupoController(GrupoService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<GrupoResponse> listar() {
        return servicio.listar();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public GrupoResponse consultar(@PathVariable Long id) {
        return servicio.consultar(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GrupoResponse> crear(
            @Valid @RequestBody GrupoRequest solicitud) {

        GrupoResponse creado = servicio.crear(solicitud);

        return ResponseEntity
                .created(URI.create("/api/v1/grupos/" + creado.id()))
                .body(creado);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public GrupoResponse actualizar(
            @PathVariable Long id,
            @Valid @RequestBody GrupoRequest solicitud) {

        return servicio.actualizar(id, solicitud);
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasRole('ADMIN')")
    public GrupoResponse cambiarEstado(
            @PathVariable Long id,
            @RequestBody Map<String, String> solicitud) {

        String estado = solicitud.get("estado");

        if (estado == null || estado.isBlank()) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El estado es obligatorio");
        }

        return servicio.cambiarEstado(id, estado);
    }
}
