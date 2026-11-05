# UniGest v0.11.0 — Sistema de gestión académica universitaria

**Java 21 · Spring Boot 4.1.1 · Maven · PostgreSQL 18 · Spring Security · Flyway · JavaScript sin frameworks**

Esta versión parte del **ZIP v0.10.0 validado** por el usuario (90 pruebas Maven en verde, portales ADMIN/DOCENTE/ESTUDIANTE revisados). La **Etapa 11** es una actualización conservadora de seguridad, calidad, accesibilidad, preparación para CI y documentación. No altera migraciones ni datos existentes.

## Funcionalidades conservadas

- Inicio de sesión con cookies de sesión, CSRF, roles ADMIN, DOCENTE y ESTUDIANTE; BCrypt y contraseñas temporales individuales.
- Portal docente limitado a sus grupos: evaluaciones, publicación, calificaciones y reportes.
- Portal estudiante limitado a sus períodos y boletines, sin notas de evaluaciones no publicadas.
- Panel administrativo: estudiantes, docentes, grupos, matrículas, programas, asignaturas, evaluaciones, calificaciones y reportes.
- Exportación CSV, impresión y guardado en PDF desde el navegador.
- Flyway `V1`–`V5`, con PostgreSQL real y desactivación lógica.

## Requisitos

- Windows 11, Java 21, PostgreSQL 18 y Maven 3.10.x (o versión compatible).
- Base de datos `unigest` previamente respaldada, con usuario y contraseña autorizados.
- No copies ni guardes contraseñas en el repositorio.

## Ejecutar pruebas y aplicación en Windows

Desde PowerShell en la carpeta donde está `pom.xml`:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\Probar-UniGest.ps1
powershell -ExecutionPolicy Bypass -File .\scripts\Iniciar-UniGest.ps1
```

Los scripts solicitan la contraseña **sin mostrarla** si `DB_PASSWORD` no está configurada. Si la URL o el usuario no son los predeterminados, configura `$env:DB_URL` y `$env:DB_USER` antes de ejecutarlos. Los scripts buscan la instalación de Maven habitual del proyecto o `mvn` en `PATH`.

Alternativa manual:

```powershell
$env:DB_URL = "jdbc:postgresql://localhost:5432/unigest"
$env:DB_USER = "postgres"
$env:DB_PASSWORD = [System.Net.NetworkCredential]::new("", (Read-Host "Contraseña PostgreSQL" -AsSecureString)).Password
& "C:\Users\stive\Tools\apache-maven-3.10.0\bin\mvn.cmd" clean test
& "C:\Users\stive\Tools\apache-maven-3.10.0\bin\mvn.cmd" spring-boot:run
```

- ADMIN: http://localhost:8080/panel/index.html
- DOCENTE/ESTUDIANTE: http://localhost:8080/portal/index.html

## Seguridad de la sesión

La configuración conserva CSRF y sesiones del servidor. Las cookies tienen `HttpOnly`, `SameSite=Lax` y una sesión de 30 minutos de inactividad. Para un despliegue con HTTPS, configura `SESSION_COOKIE_SECURE=true`. Por defecto, el servidor escucha solo en `127.0.0.1`; para publicarlo se necesita una revisión de seguridad y configuración del host/HTTPS.

## Estado de calidad

- **Base v0.10.0:** 90 pruebas en verde confirmadas previamente por el usuario.
- **v0.11.0:** dos pruebas de seguridad adicionales; **92 esperadas, todavía sin ejecutar en Windows**.
- Configuración de CI incluida en `.github/workflows/tests.yml`; requiere GitHub Actions con acceso a Maven Central.
- **No se incluye el backup de PostgreSQL en este ZIP** para evitar distribuir datos o credenciales.

Consulta [la guía de cambios y verificación de la Etapa 11](docs/ETAPA_11_CAMBIOS_Y_VERIFICACION.md). No sustituyas el ZIP y respaldo originales de v0.10.0 hasta confirmar la versión nueva.
