package com.unigest.programa;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProgramaService {
    private final ProgramaRepository repository;
    public ProgramaService(ProgramaRepository repository) { this.repository = repository; }

    @Transactional(readOnly = true)
    public List<ProgramaResponse> listarActivos() {
        return repository.findByActivoTrueOrderByNombreAsc().stream()
                .map(p -> new ProgramaResponse(p.getId(), p.getCodigo(), p.getNombre()))
                .toList();
    }
}
