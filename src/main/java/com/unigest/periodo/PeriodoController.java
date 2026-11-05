package com.unigest.periodo;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/periodos")
public class PeriodoController {

    private final PeriodoRepository periodos;

    public PeriodoController(PeriodoRepository periodos) {
        this.periodos = periodos;
    }

    @GetMapping
    public List<PeriodoResponse> listar() {
        return periodos.findAllByOrderByFechaInicioDesc()
                .stream()
                .map(periodo -> new PeriodoResponse(
                        periodo.getId(),
                        periodo.getCodigo(),
                        periodo.getNombre(),
                        periodo.getFechaInicio(),
                        periodo.getFechaFin(),
                        periodo.getEstado()
                ))
                .toList();
    }
}
