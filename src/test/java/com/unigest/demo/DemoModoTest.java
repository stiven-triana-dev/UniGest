package com.unigest.demo;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class DemoModoTest {

    @Test
    void protegeLasCuentasDePruebaSoloConLaDemoActiva() {
        var activa = new DemoModo(true, "Demo-UniGest-2026");
        var error = assertThrows(ResponseStatusException.class,
                () -> activa.protegerCuenta(" Docente@Demo.test "));
        assertEquals(HttpStatus.FORBIDDEN, error.getStatusCode());
        assertDoesNotThrow(() -> activa.protegerCuenta("v.rios@demo.test"));

        var apagada = new DemoModo(false, "Demo-UniGest-2026");
        assertDoesNotThrow(() -> apagada.protegerCuenta(DemoModo.CORREO_ADMIN));
    }

    @Test
    void exigeUnaContrasenaDeDoceCaracteresCuandoEstaActiva() {
        assertThrows(IllegalStateException.class, () -> new DemoModo(true, "corta"));
        assertDoesNotThrow(() -> new DemoModo(false, "corta"));
    }
}
