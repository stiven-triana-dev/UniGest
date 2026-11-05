-- UniGest 0.5.0
-- Ampliar la tabla de docentes creada en V1.

ALTER TABLE docentes
    ADD COLUMN documento VARCHAR(30) NOT NULL,
    ADD COLUMN fecha_vinculacion DATE NOT NULL,
    ADD CONSTRAINT uq_docentes_documento UNIQUE (documento);
