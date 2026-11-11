-- UniGest · Datos de la demo pública (todos FICTICIOS).
-- Se cargan solo con UNIGEST_DEMO=true, después de vaciar las tablas académicas
-- (ver com.unigest.demo.DemoInicializador). {{CLAVE_DEMO}} y {{CLAVE_BLOQUEADA}}
-- se reemplazan por hashes BCrypt al arrancar: ninguna contraseña queda en texto.

-- Catálogo -----------------------------------------------------------------
INSERT INTO programas (codigo, nombre) VALUES
 ('INGSIS', 'Ingeniería de Sistemas'),
 ('ADMEMP', 'Administración de Empresas'),
 ('CONPUB', 'Contaduría Pública');

INSERT INTO asignaturas (codigo, nombre, creditos) VALUES
 ('PROG101', 'Fundamentos de Programación', 3),
 ('MATE101', 'Cálculo Diferencial', 4),
 ('ESTR201', 'Estructuras de Datos', 3),
 ('BASE201', 'Bases de Datos', 3),
 ('WEB301', 'Desarrollo Web', 3),
 ('ADMI101', 'Introducción a la Administración', 3),
 ('CONT101', 'Contabilidad General', 3);

INSERT INTO planes_estudio (programa_id, asignatura_id, semestre_sugerido, obligatoria)
SELECT p.id, a.id, v.semestre, true
FROM (VALUES
 ('INGSIS', 'PROG101', 1), ('INGSIS', 'MATE101', 1), ('INGSIS', 'ESTR201', 2),
 ('INGSIS', 'BASE201', 3), ('INGSIS', 'WEB301', 4),
 ('ADMEMP', 'ADMI101', 1), ('ADMEMP', 'MATE101', 1), ('ADMEMP', 'CONT101', 2),
 ('CONPUB', 'CONT101', 1), ('CONPUB', 'ADMI101', 1)
) AS v(programa, asignatura, semestre)
JOIN programas p ON p.codigo = v.programa
JOIN asignaturas a ON a.codigo = v.asignatura;

INSERT INTO periodos (codigo, nombre, fecha_inicio, fecha_fin, estado) VALUES
 ('2026-1', 'Primer semestre de 2026', '2026-01-19', '2026-06-13', 'CERRADO'),
 ('2026-2', 'Segundo semestre de 2026', '2026-07-21', '2026-12-05', 'EN_CURSO'),
 ('2027-1', 'Primer semestre de 2027', '2027-01-18', '2027-06-12', 'PLANIFICADO');

-- Usuarios: las tres cuentas de prueba usan {{CLAVE_DEMO}}; el resto queda sin acceso.
INSERT INTO usuarios (nombres, apellidos, correo, clave_hash, requiere_cambio_clave) VALUES
 ('Administración', 'Demo', 'admin@demo.test', '{{CLAVE_DEMO}}', false),
 ('Laura', 'Méndez', 'docente@demo.test', '{{CLAVE_DEMO}}', false),
 ('Carlos', 'Rojas', 'c.rojas@demo.test', '{{CLAVE_BLOQUEADA}}', false),
 ('Marcela', 'Suárez', 'm.suarez@demo.test', '{{CLAVE_BLOQUEADA}}', false),
 ('Andrés', 'Gómez', 'estudiante@demo.test', '{{CLAVE_DEMO}}', false),
 ('Valentina', 'Ríos', 'v.rios@demo.test', '{{CLAVE_BLOQUEADA}}', false),
 ('Juan Pablo', 'Cárdenas', 'j.cardenas@demo.test', '{{CLAVE_BLOQUEADA}}', false),
 ('Daniela', 'Ortiz', 'd.ortiz@demo.test', '{{CLAVE_BLOQUEADA}}', false),
 ('Sebastián', 'Vargas', 's.vargas@demo.test', '{{CLAVE_BLOQUEADA}}', false),
 ('Mariana', 'López', 'm.lopez@demo.test', '{{CLAVE_BLOQUEADA}}', false),
 ('Camilo', 'Pérez', 'c.perez@demo.test', '{{CLAVE_BLOQUEADA}}', false),
 ('Sara', 'Jiménez', 's.jimenez@demo.test', '{{CLAVE_BLOQUEADA}}', false),
 ('Natalia', 'Ruiz', 'n.ruiz@demo.test', '{{CLAVE_BLOQUEADA}}', false);

INSERT INTO usuario_roles (usuario_id, rol_id)
SELECT u.id, r.id FROM usuarios u JOIN roles r ON r.codigo = CASE
  WHEN u.correo = 'admin@demo.test' THEN 'ADMIN'
  WHEN u.correo IN ('docente@demo.test', 'c.rojas@demo.test', 'm.suarez@demo.test') THEN 'DOCENTE'
  ELSE 'ESTUDIANTE' END;

INSERT INTO docentes (usuario_id, codigo, documento, fecha_vinculacion)
SELECT u.id, v.codigo, v.documento, v.fecha::date
FROM (VALUES
 ('docente@demo.test', 'DOC-1001', '90000001', '2021-02-01'),
 ('c.rojas@demo.test', 'DOC-1002', '90000002', '2019-08-05'),
 ('m.suarez@demo.test', 'DOC-1003', '90000003', '2023-01-16')
) AS v(correo, codigo, documento, fecha)
JOIN usuarios u ON u.correo = v.correo;

INSERT INTO estudiantes (usuario_id, codigo, programa_id, fecha_ingreso, documento)
SELECT u.id, v.codigo, p.id, v.ingreso::date, v.documento
FROM (VALUES
 ('estudiante@demo.test', '20261-0001', 'INGSIS', '2026-01-19', '10000001'),
 ('v.rios@demo.test', '20261-0002', 'INGSIS', '2026-01-19', '10000002'),
 ('j.cardenas@demo.test', '20261-0003', 'INGSIS', '2026-01-19', '10000003'),
 ('d.ortiz@demo.test', '20261-0004', 'INGSIS', '2026-01-19', '10000004'),
 ('s.vargas@demo.test', '20261-0005', 'INGSIS', '2026-01-19', '10000005'),
 ('m.lopez@demo.test', '20261-0006', 'INGSIS', '2026-01-19', '10000006'),
 ('c.perez@demo.test', '20261-0007', 'ADMEMP', '2026-01-19', '10000007'),
 ('s.jimenez@demo.test', '20261-0008', 'ADMEMP', '2026-01-19', '10000008'),
 ('n.ruiz@demo.test', '20262-0001', 'CONPUB', '2026-07-21', '10000009')
) AS v(correo, codigo, programa, ingreso, documento)
JOIN usuarios u ON u.correo = v.correo
JOIN programas p ON p.codigo = v.programa;

-- Grupos: 2026-1 cerrado, 2026-2 en curso y 2027-1 en planeación -------------
INSERT INTO grupos (asignatura_id, docente_id, periodo_id, codigo, cupo, estado)
SELECT a.id, u.id, pe.id, 'A', v.cupo, v.estado
FROM (VALUES
 ('PROG101', 'docente@demo.test', '2026-1', 30, 'CERRADO'),
 ('MATE101', 'c.rojas@demo.test', '2026-1', 35, 'CERRADO'),
 ('ESTR201', 'docente@demo.test', '2026-2', 30, 'ABIERTO'),
 ('BASE201', 'm.suarez@demo.test', '2026-2', 25, 'ABIERTO'),
 ('ADMI101', 'c.rojas@demo.test', '2026-2', 35, 'ABIERTO'),
 ('CONT101', 'c.rojas@demo.test', '2026-2', 35, 'ABIERTO'),
 ('WEB301', 'docente@demo.test', '2027-1', 25, 'BORRADOR')
) AS v(asignatura, docente, periodo, cupo, estado)
JOIN asignaturas a ON a.codigo = v.asignatura
JOIN usuarios u ON u.correo = v.docente
JOIN periodos pe ON pe.codigo = v.periodo;

INSERT INTO matriculas (estudiante_id, grupo_id, fecha, estado)
SELECT u.id, g.id, pe.fecha_inicio::timestamptz, v.estado
FROM (VALUES
 ('PROG101', '2026-1', 'estudiante@demo.test', 'FINALIZADA'), ('PROG101', '2026-1', 'v.rios@demo.test', 'FINALIZADA'),
 ('PROG101', '2026-1', 'j.cardenas@demo.test', 'FINALIZADA'), ('PROG101', '2026-1', 'd.ortiz@demo.test', 'FINALIZADA'),
 ('PROG101', '2026-1', 's.vargas@demo.test', 'FINALIZADA'), ('PROG101', '2026-1', 'm.lopez@demo.test', 'FINALIZADA'),
 ('MATE101', '2026-1', 'estudiante@demo.test', 'FINALIZADA'), ('MATE101', '2026-1', 'v.rios@demo.test', 'FINALIZADA'),
 ('MATE101', '2026-1', 'j.cardenas@demo.test', 'FINALIZADA'), ('MATE101', '2026-1', 'd.ortiz@demo.test', 'FINALIZADA'),
 ('MATE101', '2026-1', 'c.perez@demo.test', 'FINALIZADA'), ('MATE101', '2026-1', 's.jimenez@demo.test', 'FINALIZADA'),
 ('ESTR201', '2026-2', 'estudiante@demo.test', 'ACTIVA'), ('ESTR201', '2026-2', 'v.rios@demo.test', 'ACTIVA'),
 ('ESTR201', '2026-2', 'j.cardenas@demo.test', 'ACTIVA'), ('ESTR201', '2026-2', 'd.ortiz@demo.test', 'ACTIVA'),
 ('ESTR201', '2026-2', 's.vargas@demo.test', 'ACTIVA'), ('ESTR201', '2026-2', 'm.lopez@demo.test', 'RETIRADA'),
 ('BASE201', '2026-2', 'estudiante@demo.test', 'ACTIVA'), ('BASE201', '2026-2', 'v.rios@demo.test', 'ACTIVA'),
 ('BASE201', '2026-2', 's.vargas@demo.test', 'ACTIVA'), ('BASE201', '2026-2', 'm.lopez@demo.test', 'ACTIVA'),
 ('ADMI101', '2026-2', 'c.perez@demo.test', 'ACTIVA'), ('ADMI101', '2026-2', 's.jimenez@demo.test', 'ACTIVA'),
 ('ADMI101', '2026-2', 'n.ruiz@demo.test', 'ACTIVA'),
 ('CONT101', '2026-2', 'c.perez@demo.test', 'ACTIVA'), ('CONT101', '2026-2', 's.jimenez@demo.test', 'ACTIVA'),
 ('CONT101', '2026-2', 'n.ruiz@demo.test', 'ACTIVA')
) AS v(asignatura, periodo, correo, estado)
JOIN asignaturas a ON a.codigo = v.asignatura
JOIN periodos pe ON pe.codigo = v.periodo
JOIN grupos g ON g.asignatura_id = a.id AND g.periodo_id = pe.id AND g.codigo = 'A'
JOIN usuarios u ON u.correo = v.correo;

-- Evaluaciones: en cada grupo los porcentajes suman 100 %. En los grupos en curso
-- las últimas evaluaciones siguen sin publicar (el estudiante aún no ve esas notas).
INSERT INTO evaluaciones (grupo_id, titulo, ponderacion, fecha, publicada)
SELECT g.id, v.titulo, v.ponderacion, v.fecha::date, v.publicada
FROM (VALUES
 ('PROG101', '2026-1', 'Parcial 1', 30.00, '2026-03-06', true),
 ('PROG101', '2026-1', 'Parcial 2', 30.00, '2026-04-24', true),
 ('PROG101', '2026-1', 'Proyecto final', 40.00, '2026-06-05', true),
 ('MATE101', '2026-1', 'Quiz de límites', 20.00, '2026-02-27', true),
 ('MATE101', '2026-1', 'Parcial de derivadas', 35.00, '2026-04-17', true),
 ('MATE101', '2026-1', 'Examen final', 45.00, '2026-06-08', true),
 ('ESTR201', '2026-2', 'Taller de listas enlazadas', 20.00, '2026-08-21', true),
 ('ESTR201', '2026-2', 'Parcial 1', 30.00, '2026-09-18', true),
 ('ESTR201', '2026-2', 'Proyecto de árboles', 20.00, '2026-10-30', false),
 ('ESTR201', '2026-2', 'Examen final', 30.00, '2026-11-27', false),
 ('BASE201', '2026-2', 'Modelo entidad-relación', 25.00, '2026-08-28', true),
 ('BASE201', '2026-2', 'Parcial de SQL', 35.00, '2026-10-02', true),
 ('BASE201', '2026-2', 'Proyecto de base de datos', 40.00, '2026-11-20', false),
 ('ADMI101', '2026-2', 'Ensayo', 30.00, '2026-08-28', true),
 ('ADMI101', '2026-2', 'Parcial', 30.00, '2026-10-09', true),
 ('ADMI101', '2026-2', 'Caso empresarial', 40.00, '2026-11-25', false),
 ('CONT101', '2026-2', 'Taller de cuentas', 25.00, '2026-09-04', true),
 ('CONT101', '2026-2', 'Parcial', 35.00, '2026-10-16', true),
 ('CONT101', '2026-2', 'Estados financieros', 40.00, '2026-11-27', false)
) AS v(asignatura, periodo, titulo, ponderacion, fecha, publicada)
JOIN asignaturas a ON a.codigo = v.asignatura
JOIN periodos pe ON pe.codigo = v.periodo
JOIN grupos g ON g.asignatura_id = a.id AND g.periodo_id = pe.id AND g.codigo = 'A';

-- Notas: todas las evaluaciones publicadas tienen nota, y el docente ya adelantó
-- algunas de "Proyecto de árboles" (borrador, aún no visibles para estudiantes).
-- La nota sale de una fórmula fija (2.5 a 4.9) para que la demo sea igual siempre.
INSERT INTO calificaciones (matricula_id, evaluacion_id, grupo_id, nota, fecha_registro, actualizado_en)
SELECT m.id, e.id, m.grupo_id,
       round(2.5 + (abs(hashtext(u.correo || '|' || e.titulo || '|' || e.grupo_id)::bigint) % 25) / 10.0, 1),
       e.fecha::timestamptz + interval '3 days', e.fecha::timestamptz + interval '3 days'
FROM matriculas m
JOIN evaluaciones e ON e.grupo_id = m.grupo_id
JOIN usuarios u ON u.id = m.estudiante_id
WHERE m.estado IN ('ACTIVA', 'FINALIZADA')
  AND (e.publicada OR (e.titulo = 'Proyecto de árboles' AND u.correo IN ('estudiante@demo.test', 'v.rios@demo.test', 'j.cardenas@demo.test')));

-- Una corrección de nota con su motivo, para que se vea el historial.
UPDATE calificaciones c SET nota = 4.2, actualizado_en = '2026-09-25'
FROM evaluaciones e, matriculas m, usuarios u
WHERE c.evaluacion_id = e.id AND c.matricula_id = m.id AND u.id = m.estudiante_id
  AND e.titulo = 'Parcial 1' AND e.grupo_id = (SELECT g.id FROM grupos g JOIN asignaturas a ON a.id = g.asignatura_id WHERE a.codigo = 'ESTR201')
  AND u.correo = 'j.cardenas@demo.test';

INSERT INTO historial_calificaciones (calificacion_id, modificado_por, nota_anterior, nota_nueva, motivo, fecha)
SELECT c.id, (SELECT id FROM usuarios WHERE correo = 'admin@demo.test'),
       round(2.5 + (abs(hashtext('j.cardenas@demo.test|Parcial 1|' || c.grupo_id)::bigint) % 25) / 10.0, 1),
       c.nota, 'Revisión del segundo punto solicitada por el estudiante', '2026-09-25'
FROM calificaciones c
JOIN evaluaciones e ON e.id = c.evaluacion_id
JOIN matriculas m ON m.id = c.matricula_id
JOIN usuarios u ON u.id = m.estudiante_id
WHERE e.titulo = 'Parcial 1' AND u.correo = 'j.cardenas@demo.test'
  AND e.grupo_id = (SELECT g.id FROM grupos g JOIN asignaturas a ON a.id = g.asignatura_id WHERE a.codigo = 'ESTR201');
