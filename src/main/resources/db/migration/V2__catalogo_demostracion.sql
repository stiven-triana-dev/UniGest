-- Datos FICTICIOS para comenzar. No son registros de una universidad real.
INSERT INTO roles (codigo, nombre) VALUES
 ('ADMIN','Administración'),('DOCENTE','Docente'),('ESTUDIANTE','Estudiante');

INSERT INTO programas (codigo, nombre) VALUES
 ('INGSIS','Ingeniería de Sistemas'),
 ('ADMEMP','Administración de Empresas');

INSERT INTO asignaturas (codigo, nombre, creditos) VALUES
 ('PROG101','Fundamentos de Programación',3),
 ('MATE101','Cálculo Diferencial',4),
 ('BASE201','Bases de Datos',3),
 ('ADMI101','Introducción a la Administración',3);

INSERT INTO planes_estudio (programa_id, asignatura_id, semestre_sugerido, obligatoria)
SELECT p.id, a.id, 1, true FROM programas p JOIN asignaturas a ON
 (p.codigo = 'INGSIS' AND a.codigo IN ('PROG101','MATE101')) OR
 (p.codigo = 'ADMEMP' AND a.codigo = 'ADMI101');
INSERT INTO planes_estudio (programa_id, asignatura_id, semestre_sugerido, obligatoria)
SELECT p.id, a.id, 3, true FROM programas p JOIN asignaturas a ON
 p.codigo = 'INGSIS' AND a.codigo = 'BASE201';

INSERT INTO periodos (codigo, nombre, fecha_inicio, fecha_fin, estado)
VALUES ('2027-1','Primer semestre de 2027','2027-01-18','2027-06-12','PLANIFICADO');
