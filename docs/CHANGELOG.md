# Changelog

## v0.11.0

### Seguridad
- Cookie de sesión `HttpOnly` y `SameSite=Lax`, con expiración a los 30 minutos de inactividad.
- Variable `SESSION_COOKIE_SECURE` para activar el atributo `Secure` al usar HTTPS.
- La aplicación escucha por defecto en `127.0.0.1` para evitar exponer una instancia local por accidente.
- Nuevas pruebas: contraseña corta y emisión de credenciales a una cuenta inactiva.

### Calidad
- Las pruebas de integración de cupo lleno crean sus propios datos dentro de transacciones con rollback, sin depender de identificadores fijos.
- Total: 92 pruebas automatizadas.
- Flujo de GitHub Actions con Java 21 y PostgreSQL 18.

### Interfaz
- Foco visible para navegación con teclado.
- Ajustes responsive en el portal y en el modal de credenciales.
- Respeto por la preferencia de movimiento reducido.

### Operación
- Scripts de PowerShell para iniciar la aplicación y correr las pruebas sin guardar contraseñas en archivos.
- Versiones antiguas del JavaScript del panel retiradas de `static/` para no servirlas por HTTP.

### Sin cambios
- Migraciones Flyway `V1` a `V5`, lógica de cálculo de notas y autorización por rol.
