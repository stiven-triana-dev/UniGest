package com.unigest.calificacion;

import com.unigest.evaluacion.Evaluacion;
import com.unigest.evaluacion.EvaluacionRepository;
import com.unigest.grupo.GrupoRepository;
import com.unigest.matricula.Matricula;
import com.unigest.matricula.MatriculaRepository;
import com.unigest.usuario.Usuario;
import com.unigest.usuario.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CalificacionService {
    private final CalificacionRepository notas;
    private final HistorialCalificacionRepository historial;
    private final EvaluacionRepository evaluaciones;
    private final MatriculaRepository matriculas;
    private final GrupoRepository grupos;
    private final UsuarioRepository usuarios;

    public CalificacionService(CalificacionRepository notas, HistorialCalificacionRepository historial,
            EvaluacionRepository evaluaciones, MatriculaRepository matriculas,
            GrupoRepository grupos, UsuarioRepository usuarios) {
        this.notas = notas; this.historial = historial; this.evaluaciones = evaluaciones;
        this.matriculas = matriculas; this.grupos = grupos; this.usuarios = usuarios;
    }

    @Transactional(readOnly = true)
    public List<NotaEstudianteResponse> listar(Long evaluacionId) {
        Evaluacion eval = evaluacion(evaluacionId);
        Map<Long, Calificacion> byMatricula = notas.findByEvaluacionId(evaluacionId).stream()
            .collect(Collectors.toMap(n -> n.getMatricula().getId(), Function.identity()));
        return matriculas.findByGrupoIdOrderByIdAsc(eval.getGrupo().getId()).stream()
            .map(m -> {
                Calificacion n = byMatricula.get(m.getId());
                return new NotaEstudianteResponse(m.getId(), nombre(m),
                    m.getEstudiante().getCodigo(), m.getEstado(),
                    n == null ? null : n.getId(), n == null ? null : n.getNota());
            }).toList();
    }

    @Transactional
    public NotaEstudianteResponse guardar(CalificacionRequest request, String correoAdmin) {
        Evaluacion eval = evaluacion(request.evaluacionId());
        Matricula matricula = matriculas.findById(request.matriculaId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Matrícula no encontrada"));
        if (!matricula.getGrupo().getId().equals(eval.getGrupo().getId()))
            conflicto("La evaluación y matrícula deben pertenecer al mismo grupo");
        if (!"ACTIVA".equals(matricula.getEstado()))
            conflicto("Solo se permite calificar matrículas activas");
        if (request.nota().scale() > 2 && request.nota().stripTrailingZeros().scale() > 2)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La nota admite hasta dos decimales");

        Calificacion calificacion = notas.findByMatriculaIdAndEvaluacionId(
            request.matriculaId(), request.evaluacionId()).orElse(null);
        if (calificacion == null) {
            calificacion = notas.save(new Calificacion(matricula, eval, request.nota()));
        } else if (calificacion.getNota().compareTo(request.nota()) != 0) {
            String motivo = request.motivo() == null ? "" : request.motivo().trim();
            if (motivo.isEmpty())
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Para corregir una nota debes indicar el motivo");
            Usuario admin = usuarios.findByCorreoIgnoreCase(correoAdmin).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Administrador no encontrado"));
            historial.save(new HistorialCalificacion(calificacion, admin,
                calificacion.getNota(), request.nota(), motivo));
            calificacion.actualizar(request.nota());
        }
        return new NotaEstudianteResponse(matricula.getId(), nombre(matricula),
            matricula.getEstudiante().getCodigo(), matricula.getEstado(),
            calificacion.getId(), calificacion.getNota());
    }

    @Transactional(readOnly = true)
    public List<DefinitivaResponse> definitivas(Long grupoId) {
        if (!grupos.existsById(grupoId))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Grupo no encontrado");
        var evaluacionesGrupo = evaluaciones.findByGrupoIdOrderByIdAsc(grupoId);
        boolean completas = !evaluacionesGrupo.isEmpty() && evaluacionesGrupo.stream()
            .allMatch(Evaluacion::isPublicada) && evaluacionesGrupo.stream()
            .map(Evaluacion::getPonderacion).reduce(BigDecimal.ZERO, BigDecimal::add)
            .compareTo(new BigDecimal("100.00")) == 0;
        Map<Long, Map<Long, Calificacion>> porMatricula = notas.findByGrupoId(grupoId).stream()
            .collect(Collectors.groupingBy(n -> n.getMatricula().getId(),
                Collectors.toMap(n -> n.getEvaluacion().getId(), Function.identity())));
        return matriculas.findByGrupoIdOrderByIdAsc(grupoId).stream()
            .filter(m -> "ACTIVA".equals(m.getEstado()))
            .map(m -> {
                var calificaciones = porMatricula.getOrDefault(m.getId(), Map.of());
                BigDecimal definitiva = null;
                if (completas && evaluacionesGrupo.stream().allMatch(e -> calificaciones.containsKey(e.getId()))) {
                    var ponderadas = evaluacionesGrupo.stream().map(e ->
                        new CalculadoraNotas.EvaluacionCalificada(
                            calificaciones.get(e.getId()).getNota(), e.getPonderacion())).toList();
                    definitiva = CalculadoraNotas.calcularDefinitiva(ponderadas);
                }
                return new DefinitivaResponse(m.getId(), nombre(m),
                    m.getEstudiante().getCodigo(), definitiva,
                    definitiva == null ? null : CalculadoraNotas.aprobo(definitiva, new BigDecimal("3.00")));
            }).toList();
    }

    private Evaluacion evaluacion(Long id) {
        return evaluaciones.findById(id).orElseThrow(() ->
            new ResponseStatusException(HttpStatus.NOT_FOUND, "Evaluación no encontrada"));
    }
    private String nombre(Matricula m) {
        return m.getEstudiante().getUsuario().getNombres() + " " +
            m.getEstudiante().getUsuario().getApellidos();
    }
    private void conflicto(String m) { throw new ResponseStatusException(HttpStatus.CONFLICT, m); }
}
