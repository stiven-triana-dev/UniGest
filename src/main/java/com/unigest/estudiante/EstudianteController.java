package com.unigest.estudiante;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import java.net.URI;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@Validated
@RequestMapping("/api/v1/estudiantes")
public class EstudianteController {
    private final EstudianteService service;
    public EstudianteController(EstudianteService service) { this.service = service; }

    @GetMapping
    public Page<EstudianteResponse> listar(
            @RequestParam(defaultValue = "") String buscar,
            @RequestParam(required = false) @Positive Long programaId,
            @RequestParam(defaultValue = "0") @Min(0) int pagina,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int tamano) {
        return service.listar(buscar, programaId, pagina, tamano);
    }

    @GetMapping("/{id}")
    public EstudianteResponse consultar(@PathVariable @Positive Long id) {
        return service.consultar(id);
    }

    @PostMapping
    public ResponseEntity<EstudianteResponse> crear(@RequestBody @Valid EstudianteRequest solicitud) {
        var creado = service.crear(solicitud);
        return ResponseEntity.created(URI.create("/api/v1/estudiantes/" + creado.id())).body(creado);
    }

    @PutMapping("/{id}")
    public EstudianteResponse actualizar(@PathVariable @Positive Long id,
            @RequestBody @Valid EstudianteRequest solicitud) {
        return service.actualizar(id, solicitud);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desactivar(@PathVariable @Positive Long id) {
        service.desactivar(id);
        return ResponseEntity.noContent().build();
    }
}
