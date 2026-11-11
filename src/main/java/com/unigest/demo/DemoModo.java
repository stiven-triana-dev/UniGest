package com.unigest.demo;

import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * Modo demo pública (UNIGEST_DEMO=true): datos ficticios que se reinician al arrancar
 * y tres cuentas de prueba, una por rol, que nadie puede modificar desde la aplicación.
 */
@Component
public class DemoModo {

    public static final String CORREO_ADMIN = "admin@demo.test";
    public static final String CORREO_DOCENTE = "docente@demo.test";
    public static final String CORREO_ESTUDIANTE = "estudiante@demo.test";
    private static final Set<String> CUENTAS = Set.of(CORREO_ADMIN, CORREO_DOCENTE, CORREO_ESTUDIANTE);

    private final boolean activo;
    private final String clave;

    public DemoModo(
            @Value("${UNIGEST_DEMO:false}") boolean activo,
            @Value("${UNIGEST_DEMO_PASSWORD:Demo-UniGest-2026}") String clave) {
        if (activo && (clave == null || clave.length() < 12)) {
            throw new IllegalStateException("UNIGEST_DEMO_PASSWORD debe tener 12 o más caracteres");
        }
        this.activo = activo;
        this.clave = clave;
    }

    public boolean isActivo() { return activo; }

    public String getClave() { return clave; }

    public record CuentaDemo(String rol, String correo) { }

    /** Cuentas que se muestran en la pantalla de inicio de sesión. */
    public List<CuentaDemo> cuentas() {
        return List.of(
                new CuentaDemo("Administración", CORREO_ADMIN),
                new CuentaDemo("Docente", CORREO_DOCENTE),
                new CuentaDemo("Estudiante", CORREO_ESTUDIANTE));
    }

    public boolean esCuentaDemo(String correo) {
        return activo && correo != null && CUENTAS.contains(correo.trim().toLowerCase(Locale.ROOT));
    }

    /** Impide cambiar la contraseña, emitir credenciales o desactivar una cuenta de prueba. */
    public void protegerCuenta(String correo) {
        if (esCuentaDemo(correo)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "En la demo pública las cuentas de prueba no se pueden modificar. "
                    + "Los datos se reinician cada vez que la aplicación arranca.");
        }
    }
}
