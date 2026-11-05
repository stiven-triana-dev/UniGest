package com.unigest.usuario;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.Map;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
public class AccesoController {
    private final CuentaAccesoService cuentas;
    public AccesoController(CuentaAccesoService cuentas) { this.cuentas = cuentas; }

    public record CambiarClave(@NotBlank String actual, @NotBlank String nueva) { }

    @PostMapping("/api/v1/admin/accesos/{id}/credencial")
    public ResponseEntity<CuentaAccesoService.CredencialTemporal> emitir(@PathVariable Long id) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
            .body(cuentas.emitir(id));
    }

    @GetMapping("/api/v1/auth/estado-clave")
    public ResponseEntity<CuentaAccesoService.EstadoClave> estado(Authentication auth) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
            .body(new CuentaAccesoService.EstadoClave(cuentas.requiereCambio(auth.getName())));
    }

    @PostMapping("/api/v1/auth/cambiar-clave")
    public ResponseEntity<Void> cambiar(@Valid @RequestBody CambiarClave datos, Authentication auth) {
        cuentas.cambiar(auth.getName(), datos.actual(), datos.nueva());
        return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
    }
}
