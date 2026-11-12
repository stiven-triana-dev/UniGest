package com.unigest.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.tags.Tag;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Datos que muestra la documentación de la API en /swagger-ui.html. */
@Configuration
public class OpenApiConfig {

    /** Nombre legible y descripción de cada grupo de rutas, numerados en el orden en que se muestran. */
    private static final Map<String, String[]> GRUPOS = Map.ofEntries(
            Map.entry("sesion-controller", new String[] {"1. Sesión", "Inicio y cierre de sesión, token CSRF y usuario actual."}),
            Map.entry("acceso-controller", new String[] {"2. Accesos", "Credenciales de docentes y estudiantes y cambio de contraseña."}),
            Map.entry("estudiante-controller", new String[] {"3. Estudiantes", "Registro y consulta de estudiantes (administrador)."}),
            Map.entry("docente-controller", new String[] {"4. Docentes", "Registro y consulta de docentes (administrador)."}),
            Map.entry("programa-controller", new String[] {"5. Programas", "Programas universitarios."}),
            Map.entry("asignatura-controller", new String[] {"6. Asignaturas", "Materias y créditos."}),
            Map.entry("periodo-controller", new String[] {"7. Períodos", "Períodos académicos."}),
            Map.entry("grupo-controller", new String[] {"8. Grupos", "Grupos por asignatura, docente y cupos."}),
            Map.entry("matricula-controller", new String[] {"9. Matrículas", "Inscripción y retiro de estudiantes en grupos."}),
            Map.entry("evaluacion-controller", new String[] {"10. Evaluaciones", "Plan de evaluación de cada grupo (ponderaciones al 100 %)."}),
            Map.entry("calificacion-controller", new String[] {"11. Calificaciones", "Notas, correcciones auditadas y definitivas."}),
            Map.entry("reporte-academico-controller", new String[] {"12. Reportes", "Consolidados por grupo y boletines por estudiante."}),
            Map.entry("mi-espacio-controller", new String[] {"13. Mi espacio", "Portal de docentes y estudiantes: solo sus propios datos."}),
            Map.entry("estado-controller", new String[] {"14. Estado", "Salud del servicio."}),
            Map.entry("demo-controller", new String[] {"15. Demo", "Información de la demo pública."}));

    @Bean
    OpenAPI unigestOpenApi() {
        return new OpenAPI().info(new Info()
                .title("UniGest API")
                .version("0.11.0")
                .description("""
                        API REST de gestión académica: estudiantes, docentes, grupos, matrículas, \
                        evaluaciones, notas y reportes.

                        La autenticación es por sesión: inicia sesión en /panel/index.html y \
                        vuelve aquí para consultar las rutas protegidas. Las operaciones que \
                        modifican datos exigen además el token CSRF de /api/v1/auth/csrf \
                        (cabecera X-CSRF-TOKEN).""")
                .contact(new Contact().name("Stiven").url("https://github.com/stiven-triana-dev"))
                .license(new License().name("Proyecto educativo")));
    }

    /** Cambia los nombres técnicos ("grupo-controller") por nombres en español con descripción. */
    @Bean
    OpenApiCustomizer gruposEnEspanol() {
        return api -> {
            if (api.getPaths() != null) {
                api.getPaths().values().forEach(ruta -> ruta.readOperations().forEach(op -> {
                    if (op.getTags() != null) {
                        op.setTags(op.getTags().stream()
                                .map(t -> GRUPOS.containsKey(t) ? GRUPOS.get(t)[0] : t).toList());
                    }
                }));
            }
            List<Tag> tags = new ArrayList<>();
            GRUPOS.values().forEach(g -> tags.add(new Tag().name(g[0]).description(g[1])));
            tags.sort((a, b) -> Integer.compare(numero(a.getName()), numero(b.getName())));
            api.setTags(tags);
        };
    }

    private static int numero(String nombre) {
        return Integer.parseInt(nombre.substring(0, nombre.indexOf('.')));
    }
}
