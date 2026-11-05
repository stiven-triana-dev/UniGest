package com.unigest.general;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class EstadoController {
    @GetMapping("/")
    public Map<String, String> inicio() {
        return Map.of("aplicacion", "UniGest", "fase", "3 - Panel web local", "documentacion", "/api/v1/estado");
    }
    @GetMapping("/api/v1/estado")
    public Map<String, String> estado() {
        return Map.of("estado", "OK", "aplicacion", "UniGest", "version", "0.3.0");
    }
}
