package com.unigest.docente;

import jakarta.validation.Valid;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/docentes")
public class DocenteController {

    private final DocenteService servicio;

    public DocenteController(DocenteService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public List<DocenteResponse> listar() {
        return servicio.listar();
    }

    @GetMapping("/{id}")
    public DocenteResponse consultar(@PathVariable Long id) {
        return servicio.consultar(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DocenteResponse crear(@Valid @RequestBody DocenteRequest solicitud) {
        return servicio.crear(solicitud);
    }

    @PutMapping("/{id}")
    public DocenteResponse actualizar(
            @PathVariable Long id,
            @Valid @RequestBody DocenteRequest solicitud) {
        return servicio.actualizar(id, solicitud);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void desactivar(@PathVariable Long id) {
        servicio.desactivar(id);
    }
}
