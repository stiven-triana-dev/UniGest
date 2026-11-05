-- Etapa 10: las credenciales provisionales deben cambiarse antes de acceder a datos académicos.
-- No establece contraseñas por defecto ni cambia cuentas históricas.
ALTER TABLE usuarios ADD COLUMN requiere_cambio_clave BOOLEAN NOT NULL DEFAULT FALSE;
