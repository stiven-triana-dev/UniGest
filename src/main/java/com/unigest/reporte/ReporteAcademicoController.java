package com.unigest.reporte;

import com.unigest.reporte.ReporteAcademicoResponse.Boletin;
import com.unigest.reporte.ReporteAcademicoResponse.EstudianteDisponible;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

/** Endpoints de consulta: autenticación ADMIN configurada en SecurityConfig. */
@RestController
@RequestMapping("/api/v1/reportes")
public class ReporteAcademicoController {
    private final ReporteAcademicoService servicio;

    public ReporteAcademicoController(ReporteAcademicoService servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/grupos/{grupoId}")
    public ReporteAcademicoResponse.Grupo grupo(@PathVariable Long grupoId) {
        return servicio.reporteGrupo(grupoId);
    }

    @GetMapping("/periodos/{periodoId}/estudiantes")
    public List<EstudianteDisponible> estudiantes(@PathVariable Long periodoId) {
        return servicio.estudiantesPeriodo(periodoId);
    }

    @GetMapping("/boletines/estudiantes/{estudianteId}")
    public Boletin boletin(@PathVariable Long estudianteId,
                           @RequestParam Long periodoId) {
        return servicio.boletin(estudianteId, periodoId);
    }
}
