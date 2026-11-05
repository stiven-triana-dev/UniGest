package com.unigest.asignatura;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/asignaturas")
public class AsignaturaController {
    private final AsignaturaService service;
    public AsignaturaController(AsignaturaService service) { this.service = service; }

    @GetMapping
    public List<AsignaturaResponse> listar() { return service.listarActivas(); }
}
