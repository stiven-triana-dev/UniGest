package com.unigest.reporte;

import com.unigest.calificacion.CalculadoraNotas;
import com.unigest.calificacion.Calificacion;
import com.unigest.calificacion.CalificacionRepository;
import com.unigest.estudiante.Estudiante;
import com.unigest.estudiante.EstudianteRepository;
import com.unigest.evaluacion.Evaluacion;
import com.unigest.evaluacion.EvaluacionRepository;
import com.unigest.grupo.Grupo;
import com.unigest.grupo.GrupoRepository;
import com.unigest.matricula.Matricula;
import com.unigest.matricula.MatriculaRepository;
import com.unigest.periodo.Periodo;
import com.unigest.periodo.PeriodoRepository;
import com.unigest.reporte.ReporteAcademicoResponse.Boletin;
import com.unigest.reporte.ReporteAcademicoResponse.EstudianteDisponible;
import com.unigest.reporte.ReporteAcademicoResponse.EvaluacionDetalle;
import com.unigest.reporte.ReporteAcademicoResponse.FilaGrupo;
import com.unigest.reporte.ReporteAcademicoResponse.Materia;
import com.unigest.reporte.ReporteAcademicoResponse.NotaDetalle;
import com.unigest.reporte.ReporteAcademicoResponse.Resumen;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Etapa 9: reportes de solo lectura basados en las calificaciones existentes. */
@Service
@Transactional(readOnly = true)
public class ReporteAcademicoService {
    private static final BigDecimal CIEN = new BigDecimal("100.00");
    private static final BigDecimal APROBATORIA = new BigDecimal("3.00");

    private final GrupoRepository grupos;
    private final PeriodoRepository periodos;
    private final EstudianteRepository estudiantes;
    private final MatriculaRepository matriculas;
    private final EvaluacionRepository evaluaciones;
    private final CalificacionRepository calificaciones;

    public ReporteAcademicoService(GrupoRepository grupos, PeriodoRepository periodos,
            EstudianteRepository estudiantes, MatriculaRepository matriculas,
            EvaluacionRepository evaluaciones, CalificacionRepository calificaciones) {
        this.grupos = grupos;
        this.periodos = periodos;
        this.estudiantes = estudiantes;
        this.matriculas = matriculas;
        this.evaluaciones = evaluaciones;
        this.calificaciones = calificaciones;
    }

    public ReporteAcademicoResponse.Grupo reporteGrupo(Long grupoId) {
        Grupo grupo = grupos.findById(grupoId).orElseThrow(() ->
            noEncontrado("Grupo no encontrado"));
        List<Evaluacion> actividades = evaluaciones.findByGrupoIdOrderByIdAsc(grupoId);
        Map<Long, Map<Long, BigDecimal>> notas = notasPorMatricula(grupoId);

        List<FilaGrupo> filas = matriculas.findByGrupoIdOrderByIdAsc(grupoId).stream()
            .filter(m -> "ACTIVA".equals(m.getEstado()))
            .map(m -> {
                Map<Long, BigDecimal> notasEstudiante = notas.getOrDefault(m.getId(), Map.of());
                BigDecimal notaFinal = definitiva(actividades, notasEstudiante);
                return new FilaGrupo(m.getId(), m.getEstudiante().getId(),
                    m.getEstudiante().getCodigo(), nombre(m.getEstudiante()), m.getEstado(),
                    detalle(actividades, notasEstudiante), notaFinal,
                    notaFinal == null ? null : notaFinal.compareTo(APROBATORIA) >= 0);
            })
            .toList();

        List<EvaluacionDetalle> columnas = actividades.stream().map(e ->
            new EvaluacionDetalle(e.getId(), e.getTitulo(), e.getPonderacion(),
                e.getFecha(), e.isPublicada())).toList();
        return new ReporteAcademicoResponse.Grupo(grupo.getId(), grupo.getCodigo(),
            grupo.getAsignatura().getCodigo(), grupo.getAsignatura().getNombre(),
            grupo.getPeriodo().getId(), grupo.getPeriodo().getNombre(),
            grupo.getDocente().getUsuario().getNombres() + " " +
                grupo.getDocente().getUsuario().getApellidos(),
            columnas, filas, resumen(filas.stream().map(FilaGrupo::definitiva).toList()));
    }

    public List<EstudianteDisponible> estudiantesPeriodo(Long periodoId) {
        periodo(periodoId);
        Map<Long, EstudianteDisponible> unicos = new LinkedHashMap<>();
        for (Matricula m : matriculas.findByGrupoPeriodoIdOrderByIdAsc(periodoId)) {
            if (!vigenteOFinalizada(m)) continue;
            Estudiante e = m.getEstudiante();
            unicos.putIfAbsent(e.getId(),
                new EstudianteDisponible(e.getId(), e.getCodigo(), nombre(e)));
        }
        return unicos.values().stream()
            .sorted(Comparator.comparing(EstudianteDisponible::nombre,
                String.CASE_INSENSITIVE_ORDER))
            .toList();
    }

    public Boletin boletin(Long estudianteId, Long periodoId) {
        Estudiante estudiante = estudiantes.findById(estudianteId).orElseThrow(() ->
            noEncontrado("Estudiante no encontrado"));
        Periodo periodo = periodo(periodoId);
        List<Materia> materias = new ArrayList<>();
        for (Matricula m : matriculas.findByEstudianteIdOrderByIdAsc(estudianteId)) {
            if (!vigenteOFinalizada(m) ||
                !periodoId.equals(m.getGrupo().getPeriodo().getId())) continue;

            Grupo grupo = m.getGrupo();
            List<Evaluacion> actividades = evaluaciones.findByGrupoIdOrderByIdAsc(grupo.getId());
            Map<Long, BigDecimal> notas = notasPorMatricula(grupo.getId())
                .getOrDefault(m.getId(), Map.of());
            BigDecimal definitiva = definitiva(actividades, notas);
            materias.add(new Materia(grupo.getAsignatura().getCodigo(),
                grupo.getAsignatura().getNombre(), grupo.getAsignatura().getCreditos(),
                grupo.getCodigo(), m.getEstado(), detalle(actividades, notas),
                definitiva, definitiva == null ? null :
                    definitiva.compareTo(APROBATORIA) >= 0));
        }
        materias.sort(Comparator.comparing(Materia::nombre,
            String.CASE_INSENSITIVE_ORDER).thenComparing(Materia::grupoCodigo));
        return new Boletin(estudiante.getId(), estudiante.getCodigo(),
            nombre(estudiante), estudiante.getPrograma().getNombre(), periodo.getId(),
            periodo.getNombre(), List.copyOf(materias),
            resumen(materias.stream().map(Materia::definitiva).toList()));
    }

    private Periodo periodo(Long id) {
        return periodos.findById(id).orElseThrow(() ->
            noEncontrado("Período académico no encontrado"));
    }

    private boolean vigenteOFinalizada(Matricula m) {
        return "ACTIVA".equals(m.getEstado()) || "FINALIZADA".equals(m.getEstado());
    }

    private String nombre(Estudiante e) {
        return e.getUsuario().getNombres() + " " + e.getUsuario().getApellidos();
    }

    private Map<Long, Map<Long, BigDecimal>> notasPorMatricula(Long grupoId) {
        Map<Long, Map<Long, BigDecimal>> porMatricula = new HashMap<>();
        for (Calificacion c : calificaciones.findByGrupoId(grupoId)) {
            porMatricula.computeIfAbsent(c.getMatricula().getId(), k -> new HashMap<>())
                .put(c.getEvaluacion().getId(), c.getNota());
        }
        return porMatricula;
    }

    private List<NotaDetalle> detalle(List<Evaluacion> actividades,
            Map<Long, BigDecimal> notas) {
        return actividades.stream().map(e -> new NotaDetalle(e.getId(), e.getTitulo(),
            e.getPonderacion(), notas.get(e.getId()))).toList();
    }

    /** No publica definitiva cuando hay borradores, peso incompleto o notas faltantes. */
    private BigDecimal definitiva(List<Evaluacion> actividades, Map<Long, BigDecimal> notas) {
        if (actividades.isEmpty() || actividades.stream().anyMatch(e -> !e.isPublicada()) ||
            actividades.stream().map(Evaluacion::getPonderacion)
                .reduce(BigDecimal.ZERO, BigDecimal::add).compareTo(CIEN) != 0 ||
            actividades.stream().anyMatch(e -> notas.get(e.getId()) == null)) return null;

        return CalculadoraNotas.calcularDefinitiva(actividades.stream()
            .map(e -> new CalculadoraNotas.EvaluacionCalificada(
                notas.get(e.getId()), e.getPonderacion()))
            .toList());
    }

    private Resumen resumen(List<BigDecimal> definitivas) {
        int total = definitivas.size();
        List<BigDecimal> completas = definitivas.stream().filter(n -> n != null).toList();
        int aprobadas = (int) completas.stream()
            .filter(n -> n.compareTo(APROBATORIA) >= 0).count();
        int reprobadas = completas.size() - aprobadas;
        BigDecimal promedio = completas.isEmpty() ? null : completas.stream()
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .divide(BigDecimal.valueOf(completas.size()), 2, RoundingMode.HALF_UP);
        return new Resumen(total, completas.size(), aprobadas, reprobadas,
            total - completas.size(), promedio);
    }

    private ResponseStatusException noEncontrado(String mensaje) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, mensaje);
    }
}
