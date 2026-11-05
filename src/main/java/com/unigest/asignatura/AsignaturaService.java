package com.unigest.asignatura;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AsignaturaService {
    private final AsignaturaRepository repository;
    public AsignaturaService(AsignaturaRepository repository) { this.repository = repository; }

    @Transactional(readOnly = true)
    public List<AsignaturaResponse> listarActivas() {
        return repository.findByActivaTrueOrderByNombreAsc().stream()
                .map(a -> new AsignaturaResponse(a.getId(), a.getCodigo(), a.getNombre(), a.getCreditos()))
                .toList();
    }
}
