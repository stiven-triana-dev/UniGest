package com.unigest.demo;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * En modo demo vacía las tablas académicas y carga {@code demo/datos-demo.sql} cada vez
 * que la aplicación arranca. Así, lo que cambien los visitantes dura hasta el siguiente
 * reinicio (en el plan gratuito de Render, tras unos minutos sin visitas).
 */
@Component
public class DemoInicializador implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoInicializador.class);
    private static final String TABLAS = String.join(", ",
            "historial_calificaciones", "calificaciones", "evaluaciones", "matriculas", "grupos",
            "estudiantes", "docentes", "usuario_roles", "usuarios", "planes_estudio", "periodos",
            "asignaturas", "programas");

    private final DemoModo demo;
    private final JdbcTemplate jdbc;
    private final PasswordEncoder encoder;
    private final TransactionTemplate transaccion;

    public DemoInicializador(DemoModo demo, JdbcTemplate jdbc, PasswordEncoder encoder,
                             PlatformTransactionManager transacciones) {
        this.demo = demo;
        this.jdbc = jdbc;
        this.encoder = encoder;
        this.transaccion = new TransactionTemplate(transacciones);
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!demo.isActivo()) return;
        transaccion.executeWithoutResult(estado -> reiniciar());
        log.info("Demo pública cargada con datos ficticios");
    }

    /** Vacía los datos académicos y carga los de la demo. Debe llamarse dentro de una transacción. */
    public void reiniciar() {
        String sql = leerScript()
                .replace("{{CLAVE_DEMO}}", encoder.encode(demo.getClave()))
                .replace("{{CLAVE_BLOQUEADA}}", encoder.encode(claveAleatoria()));
        jdbc.execute("TRUNCATE " + TABLAS + " RESTART IDENTITY CASCADE");
        jdbc.execute((ConnectionCallback<Void>) conexion -> {
            ScriptUtils.executeSqlScript(conexion, new ByteArrayResource(sql.getBytes(StandardCharsets.UTF_8)));
            return null;
        });
    }

    private static String leerScript() {
        try (var entrada = new ClassPathResource("demo/datos-demo.sql").getInputStream()) {
            return new String(entrada.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer demo/datos-demo.sql", e);
        }
    }

    /** Contraseña que nadie conoce: esas cuentas existen para poblar la demo, no para entrar. */
    private static String claveAleatoria() {
        byte[] bytes = new byte[24];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
