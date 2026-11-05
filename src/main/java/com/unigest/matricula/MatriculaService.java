package com.unigest.matricula;

import com.unigest.estudiante.Estudiante;
import com.unigest.estudiante.EstudianteRepository;
import com.unigest.grupo.Grupo;
import com.unigest.grupo.GrupoRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class MatriculaService {

    private final MatriculaRepository matriculas;
    private final EstudianteRepository estudiantes;
    private final GrupoRepository grupos;
    private final EntityManager entityManager;

    public MatriculaService(
            MatriculaRepository matriculas,
            EstudianteRepository estudiantes,
            GrupoRepository grupos,
            EntityManager entityManager
    ) {
        this.matriculas = matriculas;
        this.estudiantes = estudiantes;
        this.grupos = grupos;
        this.entityManager = entityManager;
    }

    @Transactional(readOnly = true)
    public List<MatriculaResponse> listarPorGrupo(Long grupoId) {
        verificarGrupo(grupoId);

        return matriculas.findByGrupoIdOrderByIdAsc(grupoId)
                .stream()
                .map(MatriculaResponse::desde)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MatriculaResponse> listarPorEstudiante(Long estudianteId) {
        verificarEstudiante(estudianteId);

        return matriculas.findByEstudianteIdOrderByIdAsc(estudianteId)
                .stream()
                .map(MatriculaResponse::desde)
                .toList();
    }

    @Transactional(readOnly = true)
    public MatriculaResponse buscarPorId(Long id) {
        return MatriculaResponse.desde(buscarMatricula(id));
    }

    @Transactional
    public MatriculaResponse matricular(MatriculaRequest request) {

        // Bloqueo para evitar sobrecupos simultáneos.
        Grupo grupo = bloquearGrupo(request.grupoId());

        if (!"ABIERTO".equals(grupo.getEstado())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El grupo no está abierto para matrículas"
            );
        }

        Estudiante estudiante = verificarEstudiante(
                request.estudianteId()
        );

        if (!estudiante.getUsuario().isActivo()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El estudiante está inactivo"
            );
        }

        Matricula existente = matriculas
                .findByEstudianteIdAndGrupoId(
                        estudiante.getId(),
                        grupo.getId()
                )
                .orElse(null);

        if (existente != null
                && !"RETIRADA".equals(existente.getEstado())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El estudiante ya tiene una matrícula en este grupo"
            );
        }

        verificarCupo(grupo);

        if (existente != null) {
            existente.reactivar();
            entityManager.flush();
            return MatriculaResponse.desde(existente);
        }

        Matricula nueva = new Matricula(estudiante, grupo);
        nueva = matriculas.saveAndFlush(nueva);

        return MatriculaResponse.desde(nueva);
    }

    @Transactional
    public MatriculaResponse retirar(Long id) {
        Matricula matricula = buscarMatricula(id);

        // Todas las operaciones que afectan cupos usan
        // el mismo bloqueo de grupo.
        bloquearGrupo(matricula.getGrupo().getId());

        if (!"ACTIVA".equals(matricula.getEstado())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Solo se pueden retirar matrículas activas"
            );
        }

        matricula.retirar();
        entityManager.flush();

        return MatriculaResponse.desde(matricula);
    }

    private void verificarCupo(Grupo grupo) {
        long ocupados = matriculas.contarActivasPorGrupo(
                grupo.getId()
        );

        if (ocupados >= grupo.getCupo()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El grupo no tiene cupos disponibles"
            );
        }
    }

    private Grupo bloquearGrupo(Long id) {
        Grupo grupo = entityManager.find(
                Grupo.class,
                id,
                LockModeType.PESSIMISTIC_WRITE
        );

        if (grupo == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Grupo no encontrado"
            );
        }

        return grupo;
    }

    private Grupo verificarGrupo(Long id) {
        return grupos.findById(id).orElseThrow(
                () -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Grupo no encontrado"
                )
        );
    }

    private Estudiante verificarEstudiante(Long id) {
        return estudiantes.findById(id).orElseThrow(
                () -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Estudiante no encontrado"
                )
        );
    }

    private Matricula buscarMatricula(Long id) {
        return matriculas.findById(id).orElseThrow(
                () -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Matrícula no encontrada"
                )
        );
    }
}
