package com.unigest.evaluacion;

import com.unigest.grupo.Grupo;
import com.unigest.grupo.GrupoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.util.List;

@Service
public class EvaluacionService {
    private final EvaluacionRepository evaluaciones;
    private final GrupoRepository grupos;

    public EvaluacionService(EvaluacionRepository evaluaciones, GrupoRepository grupos) {
        this.evaluaciones = evaluaciones;
        this.grupos = grupos;
    }

    @Transactional(readOnly = true)
    public List<EvaluacionResponse> listar(Long grupoId) {
        grupo(grupoId);
        return evaluaciones.findByGrupoIdOrderByIdAsc(grupoId).stream()
            .map(EvaluacionResponse::desde).toList();
    }

    @Transactional
    public EvaluacionResponse crear(EvaluacionRequest solicitud) {
        Grupo grupo = grupo(solicitud.grupoId());
        validarGrupo(grupo);
        validarPonderacion(grupo.getId(), null, solicitud.ponderacion());
        return EvaluacionResponse.desde(evaluaciones.save(
            new Evaluacion(grupo, solicitud.titulo().trim(),
                solicitud.ponderacion(), solicitud.fecha())));
    }

    @Transactional
    public EvaluacionResponse editar(Long id, EvaluacionRequest solicitud) {
        Evaluacion actual = evaluacion(id);
        if (actual.isPublicada()) conflicto("No se puede editar una evaluación publicada");
        if (!actual.getGrupo().getId().equals(solicitud.grupoId()))
            conflicto("No se puede cambiar el grupo de la evaluación");
        validarPonderacion(actual.getGrupo().getId(), id, solicitud.ponderacion());
        actual.actualizar(solicitud.titulo().trim(), solicitud.ponderacion(), solicitud.fecha());
        return EvaluacionResponse.desde(actual);
    }

    @Transactional
    public EvaluacionResponse publicar(Long id) {
        Evaluacion actual = evaluacion(id);
        if (actual.isPublicada()) conflicto("La evaluación ya está publicada");
        List<Evaluacion> grupo = evaluaciones.findByGrupoIdOrderByIdAsc(actual.getGrupo().getId());
        BigDecimal total = grupo.stream().map(Evaluacion::getPonderacion)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (total.compareTo(new BigDecimal("100.00")) != 0)
            conflicto("Para publicar, los porcentajes del grupo deben sumar 100%");
        actual.publicar();
        return EvaluacionResponse.desde(actual);
    }

    private void validarGrupo(Grupo g) {
        if ("BORRADOR".equals(g.getEstado()))
            conflicto("El grupo debe estar abierto o cerrado, no en borrador");
    }

    private void validarPonderacion(Long grupoId, Long excluir, BigDecimal nueva) {
        BigDecimal suma = evaluaciones.findByGrupoIdOrderByIdAsc(grupoId).stream()
            .filter(e -> !e.getId().equals(excluir))
            .map(Evaluacion::getPonderacion)
            .reduce(nueva, BigDecimal::add);
        if (suma.compareTo(new BigDecimal("100.00")) > 0)
            conflicto("Las ponderaciones del grupo no pueden superar 100%");
    }

    private Grupo grupo(Long id) {
        return grupos.findById(id).orElseThrow(() -> new ResponseStatusException(
            HttpStatus.NOT_FOUND, "Grupo no encontrado"));
    }
    private Evaluacion evaluacion(Long id) {
        return evaluaciones.findById(id).orElseThrow(() -> new ResponseStatusException(
            HttpStatus.NOT_FOUND, "Evaluación no encontrada"));
    }
    private void conflicto(String m) { throw new ResponseStatusException(HttpStatus.CONFLICT, m); }
}
