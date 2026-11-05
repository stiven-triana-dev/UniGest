package com.unigest.portal;

import com.unigest.calificacion.CalificacionRequest;
import com.unigest.calificacion.NotaEstudianteResponse;
import com.unigest.evaluacion.EvaluacionRequest;
import com.unigest.evaluacion.EvaluacionResponse;
import com.unigest.reporte.ReporteAcademicoResponse;
import com.unigest.usuario.CuentaAccesoService;
import org.springframework.web.server.ResponseStatusException;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/** El alumno nunca elige estudianteId, ni el docente elige su identidad. */
@RestController
@RequestMapping("/api/v1/mi-espacio")
public class MiEspacioController {
    private final MiEspacioService servicio;
    private final CuentaAccesoService cuentas;

    public MiEspacioController(MiEspacioService servicio, CuentaAccesoService cuentas) {
        this.servicio = servicio;
        this.cuentas = cuentas;
    }

    /** Comprueba la clave en el servidor, sin depender de la resolución de beans en SpEL. */
    private void exigirClaveDefinitiva(Authentication autenticacion) {
        if (!cuentas.claveDefinitiva(autenticacion.getName())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "Cambia la contraseña provisional antes de consultar datos académicos");
        }
    }

    @GetMapping("/docente/grupos")
    @PreAuthorize("hasRole('DOCENTE')")
    public List<MiEspacioService.GrupoAsignado> grupos(Authentication a) {
        exigirClaveDefinitiva(a);
        return servicio.misGrupos(a.getName());
    }

    @GetMapping("/docente/grupos/{id}/reporte")
    @PreAuthorize("hasRole('DOCENTE')")
    public ReporteAcademicoResponse.Grupo reporte(@PathVariable Long id, Authentication a) {
        exigirClaveDefinitiva(a);
        return servicio.reporteMiGrupo(a.getName(), id);
    }

    @GetMapping("/docente/grupos/{id}/evaluaciones")
    @PreAuthorize("hasRole('DOCENTE')")
    public List<EvaluacionResponse> evaluaciones(@PathVariable Long id, Authentication a) {
        exigirClaveDefinitiva(a);
        return servicio.evaluacionesMiGrupo(a.getName(), id);
    }

    @GetMapping("/docente/evaluaciones/{id}/notas")
    @PreAuthorize("hasRole('DOCENTE')")
    public List<NotaEstudianteResponse> notas(@PathVariable Long id, Authentication a) {
        exigirClaveDefinitiva(a);
        return servicio.notasMiEvaluacion(a.getName(), id);
    }

    @PostMapping("/docente/evaluaciones")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('DOCENTE')")
    public EvaluacionResponse crear(@Valid @RequestBody EvaluacionRequest r, Authentication a) {
        exigirClaveDefinitiva(a);
        return servicio.crearEvaluacion(a.getName(), r);
    }

    @PutMapping("/docente/evaluaciones/{id}")
    @PreAuthorize("hasRole('DOCENTE')")
    public EvaluacionResponse editar(@PathVariable Long id,
            @Valid @RequestBody EvaluacionRequest r, Authentication a) {
        exigirClaveDefinitiva(a);
        return servicio.editarEvaluacion(a.getName(), id, r);
    }

    @PatchMapping("/docente/evaluaciones/{id}/publicar")
    @PreAuthorize("hasRole('DOCENTE')")
    public EvaluacionResponse publicar(@PathVariable Long id, Authentication a) {
        exigirClaveDefinitiva(a);
        return servicio.publicarEvaluacion(a.getName(), id);
    }

    @PostMapping("/docente/calificaciones")
    @PreAuthorize("hasRole('DOCENTE')")
    public NotaEstudianteResponse guardar(@Valid @RequestBody CalificacionRequest r, Authentication a) {
        exigirClaveDefinitiva(a);
        return servicio.calificar(a.getName(), r);
    }

    @GetMapping("/estudiante/periodos")
    @PreAuthorize("hasRole('ESTUDIANTE')")
    public List<MiEspacioService.PeriodoDisponible> periodos(Authentication a) {
        exigirClaveDefinitiva(a);
        return servicio.misPeriodos(a.getName());
    }

    @GetMapping("/estudiante/boletin")
    @PreAuthorize("hasRole('ESTUDIANTE')")
    public ReporteAcademicoResponse.Boletin boletin(@RequestParam Long periodoId, Authentication a) {
        exigirClaveDefinitiva(a);
        return servicio.miBoletin(a.getName(), periodoId);
    }
}
