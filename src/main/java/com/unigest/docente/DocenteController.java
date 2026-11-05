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

    // Consultar docentes activos.
    @GetMapping
    public List<DocenteResponse> listar() {
        return servicio.listar();
    }

    // Consultar un docente por su identificador.
    @GetMapping("/{id}")
    public DocenteResponse consultar(@PathVariable Long id) {
        return servicio.consultar(id);
    }

    // Registrar un nuevo docente.
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DocenteResponse crear(@Valid @RequestBody DocenteRequest solicitud) {
        return servicio.crear(solicitud);
    }

    // Actualizar los datos de un docente.
    @PutMapping("/{id}")
    public DocenteResponse actualizar(
            @PathVariable Long id,
            @Valid @RequestBody DocenteRequest solicitud) {
        return servicio.actualizar(id, solicitud);
    }

    // Desactivar un docente sin eliminar su historial.
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void desactivar(@PathVariable Long id) {
        servicio.desactivar(id);
    }
}
