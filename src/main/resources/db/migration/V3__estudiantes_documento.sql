-- Etapa 2: registrar documento estudiantil sin modificar migraciones aplicadas.
-- Tabla estudiantes sigue vacía en la etapa 1 y el nuevo campo puede ser NOT NULL.
ALTER TABLE estudiantes ADD COLUMN documento VARCHAR(30) NOT NULL;
ALTER TABLE estudiantes ADD CONSTRAINT uq_estudiantes_documento UNIQUE (documento);
ALTER TABLE estudiantes ADD CONSTRAINT ck_estudiantes_documento CHECK (length(trim(documento)) > 0);
