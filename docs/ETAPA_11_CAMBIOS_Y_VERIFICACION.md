# UniGest v0.11.0 — Etapa 11 (candidato de verificación)

## Punto de partida comprobado por el usuario

- v0.10.0: **90 pruebas Maven aprobadas** en Windows con PostgreSQL 18.
- Panel ADMIN, portal DOCENTE y portal ESTUDIANTE comprobados manualmente.
- Boletín de estudiante con definitiva 4,74; bloqueo HTTP 403 de ruta administrativa desde sesión ESTUDIANTE.
- Código y base de datos respaldados antes de iniciar esta etapa.

## Cambios implementados

1. **Presentación:** etiqueta de versión del panel actualizada a 0.11.0; foco visible para navegación con teclado; ajustes de diseño responsive en el portal y en el modal de credenciales; respeto por preferencias de movimiento reducido.
2. **Publicación:** dos versiones antiguas de JavaScript se trasladaron de `static/panel/js/` a `archive/legacy-frontend/` para no distribuirlas mediante HTTP. El archivo `app.js` activo no cambió.
3. **Sesión:** cookie `HttpOnly`, `SameSite=Lax`, sesión de 30 minutos; variable `SESSION_COOKIE_SECURE` permite activar `Secure` cuando se use HTTPS. **En producción con HTTPS, configúrala en `true`.** El servidor sigue vinculado por defecto a `127.0.0.1`, para evitar publicar accidentalmente la instancia local.
4. **Calidad:** los dos tests de integración de cupo lleno conservan sus nombres y verificaciones, pero generan datos propios en transacciones con rollback, sin depender de `grupo_id=1`, `usuario_id=8` ni de datos privados.
5. **Pruebas de seguridad:** dos casos nuevos para contraseña corta y emisión a cuenta inactiva. Conteo esperado: **92** pruebas, pendiente de ejecución real.
6. **Operación:** scripts PowerShell para iniciar y probar sin guardar la contraseña en archivos; workflow de GitHub Actions con PostgreSQL 18 y Java 21.
7. **Documentación:** README actualizado y guía de verificación.

## Qué NO se modificó

- Ninguna migración `V1` a `V5` ni la estructura de la base de datos.
- Ningún dato ni credencial del backup PostgreSQL.
- La lógica de cálculo de notas, API administrativa, autorización por rol, ni el flujo de emisión y cambio de contraseñas.
- Las pruebas originales no se eliminaron; los dos tests de integración se aislaron de IDs fijos.

## Pruebas obligatorias en el equipo del usuario

Desde la raíz, con Java 21, Maven y PostgreSQL 18 disponibles:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\Probar-UniGest.ps1
```

Esperado: `Tests run: 92, Failures: 0, Errors: 0, Skipped: 0` y `BUILD SUCCESS`.

Si falla, comparte el error completo; no sustituyas el proyecto funcional antes de resolverlo. Para ejecutar:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\Iniciar-UniGest.ps1
```

Revisiones manuales después de pasar las pruebas:

- Administrador: abrir panel; listados, credenciales, modal alineado y navegación por teclado.
- Docente: cambio de clave temporal, grupos asignados, evaluaciones, notas.
- Estudiante: boletín y salida para imprimir; verificar que `/api/v1/estudiantes` responda 403.
- Móvil: probar 375 px de ancho, botones del modal, tarjetas, tablas desplazables y barra superior.
- Sesión: tras 30 minutos sin actividad, confirmar que el acceso autenticado exija reiniciar sesión.
- Con HTTPS real, configurar `SESSION_COOKIE_SECURE=true`; probar inicio de sesión y logout.

## Advertencias y próximos pasos

La ejecución de Maven y una conexión a PostgreSQL no estaban disponibles en este entorno de preparación. Por tanto, **esta versión es un candidato y NO puede declararse con 92 pruebas aprobadas** hasta correr el comando en Windows. La verificación de sintaxis JavaScript y la comprobación estática de los archivos no sustituyen Maven.

No se realiza restauración automática del `.backup`, ni despliegue público. Las contraseñas temporales visibles en capturas deben rotarse; evita publicarlas en el repositorio. Para producción se recomienda definir un dominio, HTTPS, usuario PostgreSQL de privilegios mínimos, gestión de secretos y políticas de acceso y copias de seguridad.
