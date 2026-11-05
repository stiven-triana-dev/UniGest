
package com.unigest.matricula;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/matriculas")
@PreAuthorize("hasRole('ADMIN')")
public class MatriculaController {

    private final MatriculaService service;

    public MatriculaController(MatriculaService service) {
        this.service = service;
    }

    @GetMapping("/grupo/{grupoId}")
    public List<MatriculaResponse> listarPorGrupo(
            @PathVariable Long grupoId
    ) {
        return service.listarPorGrupo(grupoId);
    }

    @GetMapping("/estudiante/{estudianteId}")
    public List<MatriculaResponse> listarPorEstudiante(
            @PathVariable Long estudianteId
    ) {
        return service.listarPorEstudiante(estudianteId);
    }

    @GetMapping("/{id}")
    public MatriculaResponse buscarPorId(@PathVariable Long id) {
        return service.buscarPorId(id);
    }

    @PostMapping
    public ResponseEntity<MatriculaResponse> matricular(
            @Valid @RequestBody MatriculaRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.matricular(request));
    }

    @PatchMapping("/{id}/retirar")
    public MatriculaResponse retirar(@PathVariable Long id) {
        return service.retirar(id);
    }
}
