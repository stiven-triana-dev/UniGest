package com.unigest.evaluacion;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/evaluaciones")
@PreAuthorize("hasRole('ADMIN')")
public class EvaluacionController {
    private final EvaluacionService service;
    public EvaluacionController(EvaluacionService service) { this.service = service; }
    @GetMapping("/grupo/{id}")
    public List<EvaluacionResponse> grupo(@PathVariable Long id) { return service.listar(id); }
    @PostMapping
    public ResponseEntity<EvaluacionResponse> crear(@Valid @RequestBody EvaluacionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(request));
    }
    @PutMapping("/{id}")
    public EvaluacionResponse editar(@PathVariable Long id,
            @Valid @RequestBody EvaluacionRequest request) { return service.editar(id, request); }
    @PatchMapping("/{id}/publicar")
    public EvaluacionResponse publicar(@PathVariable Long id) { return service.publicar(id); }
}
