package com.unigest.programa;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/programas")
public class ProgramaController {
    private final ProgramaService service;
    public ProgramaController(ProgramaService service) { this.service = service; }

    @GetMapping
    public List<ProgramaResponse> listar() { return service.listarActivos(); }
}
