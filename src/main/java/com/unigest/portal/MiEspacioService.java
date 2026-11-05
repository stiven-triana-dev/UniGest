package com.unigest.portal;

import com.unigest.calificacion.CalificacionRequest;
import com.unigest.calificacion.CalificacionService;
import com.unigest.calificacion.NotaEstudianteResponse;
import com.unigest.estudiante.Estudiante;
import com.unigest.estudiante.EstudianteRepository;
import com.unigest.evaluacion.Evaluacion;
import com.unigest.evaluacion.EvaluacionRepository;
import com.unigest.evaluacion.EvaluacionRequest;
import com.unigest.evaluacion.EvaluacionResponse;
import com.unigest.evaluacion.EvaluacionService;
import com.unigest.grupo.Grupo;
import com.unigest.grupo.GrupoRepository;
import com.unigest.matricula.Matricula;
import com.unigest.matricula.MatriculaRepository;
import com.unigest.reporte.ReporteAcademicoResponse;
import com.unigest.reporte.ReporteAcademicoService;
import com.unigest.usuario.Usuario;
import com.unigest.usuario.UsuarioRepository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** El ámbito de acceso se determina en el servidor a partir de la sesión. */
@Service
@Transactional(readOnly = true)
public class MiEspacioService {

    private final UsuarioRepository usuarios;
    private final EstudianteRepository estudiantes;
    private final GrupoRepository grupos;
    private final MatriculaRepository matriculas;
    private final EvaluacionRepository evaluaciones;
    private final EvaluacionService gestionEvaluaciones;
    private final CalificacionService gestionCalificaciones;
    private final ReporteAcademicoService reportes;

    public MiEspacioService(
            UsuarioRepository usuarios,
            EstudianteRepository estudiantes,
            GrupoRepository grupos,
            MatriculaRepository matriculas,
            EvaluacionRepository evaluaciones,
            EvaluacionService gestionEvaluaciones,
            CalificacionService gestionCalificaciones,
            ReporteAcademicoService reportes) {

        this.usuarios = usuarios;
        this.estudiantes = estudiantes;
        this.grupos = grupos;
        this.matriculas = matriculas;
        this.evaluaciones = evaluaciones;
        this.gestionEvaluaciones = gestionEvaluaciones;
        this.gestionCalificaciones = gestionCalificaciones;
        this.reportes = reportes;
    }

    public record GrupoAsignado(
            Long id,
            String codigo,
            String asignatura,
            String periodo,
            String estado) {
    }

    public record PeriodoDisponible(
            Long id,
            String codigo,
            String nombre) {
    }

    private Usuario usuario(String correo) {
        return usuarios.findByCorreoIgnoreCase(correo)
            .filter(Usuario::isActivo)
            .orElseThrow(() ->
                new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Cuenta no disponible"
                )
            );
    }

    private Grupo grupoDelDocente(String correo, Long id) {
        Long docenteId = usuario(correo).getId();

        return grupos.findById(id)
            .filter(g ->
                g.getDocente().getId().equals(docenteId)
            )
            .orElseThrow(() ->
                new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Grupo no asignado"
                )
            );
    }

    private Evaluacion evaluacionDelDocente(
            String correo,
            Long id) {

        Evaluacion e = evaluaciones.findById(id)
            .orElseThrow(() ->
                new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Evaluación no encontrada"
                )
            );

        grupoDelDocente(correo, e.getGrupo().getId());

        return e;
    }

    private Estudiante estudianteActual(String correo) {
        Long id = usuario(correo).getId();

        return estudiantes.findById(id)
            .filter(e -> e.getUsuario().isActivo())
            .orElseThrow(() ->
                new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "No existe un perfil estudiantil activo"
                )
            );
    }

    public List<GrupoAsignado> misGrupos(String correo) {
        Long id = usuario(correo).getId();

        return grupos.findByDocenteId(id).stream()
            .map(g -> new GrupoAsignado(
                g.getId(),
                g.getCodigo(),
                g.getAsignatura().getNombre(),
                g.getPeriodo().getNombre(),
                g.getEstado()
            ))
            .sorted(Comparator.comparing(
                GrupoAsignado::codigo
            ))
            .toList();
    }

    public ReporteAcademicoResponse.Grupo reporteMiGrupo(
            String correo,
            Long grupoId) {

        grupoDelDocente(correo, grupoId);

        return reportes.reporteGrupo(grupoId);
    }

    public List<EvaluacionResponse> evaluacionesMiGrupo(
            String correo,
            Long grupoId) {

        grupoDelDocente(correo, grupoId);

        return gestionEvaluaciones.listar(grupoId);
    }

    public List<NotaEstudianteResponse> notasMiEvaluacion(
            String correo,
            Long evaluacionId) {

        evaluacionDelDocente(correo, evaluacionId);

        return gestionCalificaciones.listar(evaluacionId);
    }

    @Transactional
    public EvaluacionResponse crearEvaluacion(
            String correo,
            EvaluacionRequest request) {

        grupoDelDocente(correo, request.grupoId());

        return gestionEvaluaciones.crear(request);
    }

    @Transactional
    public EvaluacionResponse editarEvaluacion(
            String correo,
            Long id,
            EvaluacionRequest request) {

        evaluacionDelDocente(correo, id);
        grupoDelDocente(correo, request.grupoId());

        return gestionEvaluaciones.editar(id, request);
    }

    @Transactional
    public EvaluacionResponse publicarEvaluacion(
            String correo,
            Long id) {

        evaluacionDelDocente(correo, id);

        return gestionEvaluaciones.publicar(id);
    }

    @Transactional
    public NotaEstudianteResponse calificar(
            String correo,
            CalificacionRequest request) {

        evaluacionDelDocente(
            correo,
            request.evaluacionId()
        );

        return gestionCalificaciones.guardar(
            request,
            correo
        );
    }

    public List<PeriodoDisponible> misPeriodos(
            String correo) {

        Estudiante estudiante = estudianteActual(correo);

        Map<Long, PeriodoDisponible> unicos =
            new HashMap<>();

        for (Matricula m :
                matriculas.findByEstudianteIdOrderByIdAsc(
                    estudiante.getId())) {

            if (!vigente(m)) {
                continue;
            }

            var p = m.getGrupo().getPeriodo();

            unicos.putIfAbsent(
                p.getId(),
                new PeriodoDisponible(
                    p.getId(),
                    p.getCodigo(),
                    p.getNombre()
                )
            );
        }

        return unicos.values().stream()
            .sorted(Comparator.comparing(
                PeriodoDisponible::codigo
            ))
            .toList();
    }

    /** El estudiante solo ve SUS notas publicadas, nunca borradores. */
    public ReporteAcademicoResponse.Boletin miBoletin(
            String correo,
            Long periodoId) {

        Estudiante estudiante = estudianteActual(correo);

        ReporteAcademicoResponse.Boletin original =
            reportes.boletin(
                estudiante.getId(),
                periodoId
            );

        Map<String, Set<Long>> visibles =
            new HashMap<>();

        for (Matricula m :
                matriculas.findByEstudianteIdOrderByIdAsc(
                    estudiante.getId())) {

            if (!vigente(m) ||
                !periodoId.equals(
                    m.getGrupo().getPeriodo().getId()
                )) {
                continue;
            }

            Grupo g = m.getGrupo();

            Set<Long> publicados = new HashSet<>();

            for (Evaluacion e :
                    evaluaciones.findByGrupoIdOrderByIdAsc(
                        g.getId())) {

                if (e.isPublicada()) {
                    publicados.add(e.getId());
                }
            }

            visibles.put(
                clave(
                    g.getCodigo(),
                    g.getAsignatura().getCodigo()
                ),
                publicados
            );
        }

        List<ReporteAcademicoResponse.Materia> materias =
            new ArrayList<>();

        for (var materia : original.materias()) {

            Set<Long> publicadas = visibles.getOrDefault(
                clave(
                    materia.grupoCodigo(),
                    materia.codigo()
                ),
                Set.of()
            );

            var notas = materia.notas().stream()
                .filter(n ->
                    publicadas.contains(
                        n.evaluacionId()
                    )
                )
                .toList();

            materias.add(
                new ReporteAcademicoResponse.Materia(
                    materia.codigo(),
                    materia.nombre(),
                    materia.creditos(),
                    materia.grupoCodigo(),
                    materia.estadoMatricula(),
                    notas,
                    materia.definitiva(),
                    materia.aprobado()
                )
            );
        }
        return new ReporteAcademicoResponse.Boletin(
            original.estudianteId(),
            original.estudianteCodigo(),
            original.estudianteNombre(),
            original.programa(),
            original.periodoId(),
            original.periodoNombre(),
            List.copyOf(materias),
            original.resumen()
        );
    }
    private boolean vigente(Matricula m) {
        return "ACTIVA".equals(m.getEstado())
            || "FINALIZADA".equals(m.getEstado());
    }

    private String clave(
            String grupo,
            String asignatura) {

        return grupo + "|" + asignatura;
    }
}