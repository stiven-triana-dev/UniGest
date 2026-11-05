package com.unigest.grupo;

import com.unigest.asignatura.Asignatura;
import com.unigest.asignatura.AsignaturaRepository;
import com.unigest.docente.Docente;
import com.unigest.docente.DocenteRepository;
import com.unigest.periodo.Periodo;
import com.unigest.periodo.PeriodoRepository;
import com.unigest.usuario.Usuario;

import java.util.List;
import java.util.Locale;

import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class GrupoService {

    private final GrupoRepository grupos;
    private final AsignaturaRepository asignaturas;
    private final DocenteRepository docentes;
    private final PeriodoRepository periodos;

    public GrupoService(
            GrupoRepository grupos,
            AsignaturaRepository asignaturas,
            DocenteRepository docentes,
            PeriodoRepository periodos) {

        this.grupos = grupos;
        this.asignaturas = asignaturas;
        this.docentes = docentes;
        this.periodos = periodos;
    }

    @Transactional(readOnly = true)
    public List<GrupoResponse> listar() {
        return grupos.findAll()
                .stream()
                .map(this::convertir)
                .toList();
    }

    @Transactional(readOnly = true)
    public GrupoResponse consultar(Long id) {
        return convertir(buscarGrupo(id));
    }

    @Transactional
    public GrupoResponse crear(GrupoRequest solicitud) {

        validarSolicitud(solicitud);

        Asignatura asignatura =
                buscarAsignatura(solicitud.asignaturaId());

        Docente docente =
                buscarDocenteActivo(solicitud.docenteId());

        Periodo periodo =
                buscarPeriodo(solicitud.periodoId());

        String codigo = normalizarCodigo(solicitud.codigo());

        if (grupos.existsByPeriodoIdAndAsignaturaIdAndCodigo(
                periodo.getId(),
                asignatura.getId(),
                codigo)) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Ya existe un grupo con ese código "
                    + "para la asignatura y período");
        }

        Grupo grupo = new Grupo(
                asignatura,
                docente,
                periodo,
                codigo,
                solicitud.cupo());

        return convertir(grupos.save(grupo));
    }

    @Transactional
    public GrupoResponse actualizar(
            Long id,
            GrupoRequest solicitud) {

        validarSolicitud(solicitud);

        Grupo grupo = buscarGrupo(id);

        if ("CERRADO".equals(grupo.getEstado())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "No se puede modificar un grupo cerrado");
        }

        if (!grupo.getAsignatura().getId()
                    .equals(solicitud.asignaturaId())
                || !grupo.getPeriodo().getId()
                    .equals(solicitud.periodoId())) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "No se permite cambiar la asignatura "
                    + "ni el período del grupo");
        }

        Docente docente =
                buscarDocenteActivo(solicitud.docenteId());

        String codigo = normalizarCodigo(solicitud.codigo());

        if (grupos.existsByPeriodoIdAndAsignaturaIdAndCodigoAndIdNot(
                grupo.getPeriodo().getId(),
                grupo.getAsignatura().getId(),
                codigo,
                id)) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El código del grupo ya existe");
        }

        grupo.actualizarDatos(
                docente,
                codigo,
                solicitud.cupo());

        return convertir(grupo);
    }

    @Transactional
    public GrupoResponse cambiarEstado(Long id, String nuevoEstado) {

        Grupo grupo = buscarGrupo(id);

        String estado = normalizarCodigo(nuevoEstado);

        if (!estado.equals("BORRADOR")
                && !estado.equals("ABIERTO")
                && !estado.equals("CERRADO")) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Estado de grupo no válido");
        }

        if ("CERRADO".equals(grupo.getEstado())
                && !"CERRADO".equals(estado)) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "No se puede reabrir un grupo cerrado");
        }

        if ("ABIERTO".equals(estado)
                && !grupo.getDocente().getUsuario().isActivo()) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "No se puede abrir un grupo "
                    + "con un docente inactivo");
        }

        grupo.cambiarEstado(estado);

        return convertir(grupo);
    }

    private Grupo buscarGrupo(Long id) {
        return grupos.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Grupo no encontrado"));
    }

    private Asignatura buscarAsignatura(Long id) {

        Asignatura asignatura = asignaturas.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Asignatura no encontrada"));

        if (!asignatura.isActiva()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "La asignatura está inactiva");
        }

        return asignatura;
    }

    private Docente buscarDocenteActivo(Long id) {

        return docentes.findById(id)
                .filter(d -> d.getUsuario().isActivo())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Docente activo no encontrado"));
    }

    private Periodo buscarPeriodo(Long id) {

        Periodo periodo = periodos.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Período académico no encontrado"));

        if ("CERRADO".equals(periodo.getEstado())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El período académico está cerrado");
        }

        return periodo;
    }

    private void validarSolicitud(GrupoRequest solicitud) {

        if (solicitud == null
                || solicitud.asignaturaId() == null
                || solicitud.docenteId() == null
                || solicitud.periodoId() == null
                || solicitud.cupo() == null) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Faltan datos obligatorios");
        }

        int cupo = solicitud.cupo();

        if (cupo < 1 || cupo > 1000) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El cupo debe estar entre 1 y 1000");
        }

        normalizarCodigo(solicitud.codigo());
    }

    private GrupoResponse convertir(Grupo grupo) {

        Usuario usuario = grupo.getDocente().getUsuario();

        String docenteNombre =
                usuario.getNombres() + " " + usuario.getApellidos();

        return new GrupoResponse(
                grupo.getId(),
                grupo.getAsignatura().getId(),
                grupo.getAsignatura().getNombre(),
                grupo.getDocente().getId(),
                docenteNombre,
                grupo.getPeriodo().getId(),
                grupo.getPeriodo().getNombre(),
                grupo.getCodigo(),
                grupo.getCupo(),
                grupo.getEstado());
    }

    private static String normalizarCodigo(String codigo) {

        if (codigo == null || codigo.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El código es obligatorio");
        }

        String resultado = codigo.trim()
                .replaceAll("\\s+", " ")
                .toUpperCase(Locale.ROOT);

        if (resultado.length() > 15) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El código no puede superar 15 caracteres");
        }

        return resultado;
    }
}
