package com.unigest.calificacion;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/calificaciones")
@PreAuthorize("hasRole('ADMIN')")
public class CalificacionController {
    private final CalificacionService service;
    public CalificacionController(CalificacionService service) { this.service = service; }
    @GetMapping("/evaluacion/{id}")
    public List<NotaEstudianteResponse> listado(@PathVariable Long id) { return service.listar(id); }
    @GetMapping("/definitivas/grupo/{id}")
    public List<DefinitivaResponse> definitivas(@PathVariable Long id) { return service.definitivas(id); }
    @PostMapping
    public ResponseEntity<NotaEstudianteResponse> guardar(@Valid @RequestBody CalificacionRequest request,
            Authentication authentication) {
        return ResponseEntity.status(HttpStatus.OK).body(service.guardar(request, authentication.getName()));
    }
}
