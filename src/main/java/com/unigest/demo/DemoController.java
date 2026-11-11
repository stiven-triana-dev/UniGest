package com.unigest.demo;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Informa a la pantalla de inicio de sesión si es la demo pública y con qué cuentas entrar. */
@RestController
@RequestMapping("/api/v1/demo")
public class DemoController {

    private final DemoModo demo;

    public DemoController(DemoModo demo) {
        this.demo = demo;
    }

    public record DemoResponse(boolean activo, String clave, List<DemoModo.CuentaDemo> cuentas) { }

    @GetMapping
    public DemoResponse estado() {
        if (!demo.isActivo()) return new DemoResponse(false, null, List.of());
        return new DemoResponse(true, demo.getClave(), demo.cuentas());
    }
}
