package com.unigest.general;

import java.net.URI;
import java.util.Map;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.info.BuildProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class EstadoController {
    private final String version;

    public EstadoController(ObjectProvider<BuildProperties> compilacion) {
        this.version = Version.de(compilacion);
    }

    @GetMapping("/")
    public ResponseEntity<Void> inicio() {
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create("/panel/index.html")).build();
    }

    @GetMapping("/api/v1/estado")
    public Map<String, String> estado() {
        return Map.of("estado", "OK", "aplicacion", "UniGest", "version", version);
    }
}
