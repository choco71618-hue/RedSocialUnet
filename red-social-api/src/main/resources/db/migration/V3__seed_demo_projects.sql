-- ============================================================
-- V3 — Proyectos de ejemplo precargados (coworking, laboratorio,
-- Mario Kart, Hackaton 2027 y techo del C).
-- Idempotente: sólo inserta si el título no existe todavía.
-- Portable PostgreSQL 16 + H2 2.x (MODE=PostgreSQL).
-- ============================================================

-- Sistema de coworking en la UNET
INSERT INTO projects (creator_id, kind, title, description, status, created_at, updated_at)
SELECT COALESCE((SELECT id FROM users ORDER BY id LIMIT 1 OFFSET 0), u.id),
       'PROJECT',
       'Sistema de coworking en la UNET para colaborar y trabajar en conjunto',
       'Espacio compartido para estudiar, emprender y sacar proyectos adelante entre estudiantes y profesores del Edificio C.',
       'IN_PROGRESS', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM users u
WHERE u.id = (SELECT MIN(id) FROM users)
  AND NOT EXISTS (SELECT 1 FROM projects p
                  WHERE p.title = 'Sistema de coworking en la UNET para colaborar y trabajar en conjunto');

-- Laboratorio de impresión y prototipado público
INSERT INTO projects (creator_id, kind, title, description, status, created_at, updated_at)
SELECT COALESCE((SELECT id FROM users ORDER BY id LIMIT 1 OFFSET 1), u.id),
       'PROJECT',
       'Laboratorio de impresion y prototipado publico',
       'Montar un laboratorio abierto con impresoras 3D y herramientas para que cualquiera pueda prototipar sus ideas.',
       'PROPOSED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM users u
WHERE u.id = (SELECT MIN(id) FROM users)
  AND NOT EXISTS (SELECT 1 FROM projects p
                  WHERE p.title = 'Laboratorio de impresion y prototipado publico');

-- Evento de Mario Kart
INSERT INTO projects (creator_id, kind, title, description, event_date, location, created_at, updated_at)
SELECT COALESCE((SELECT id FROM users ORDER BY id LIMIT 1 OFFSET 2), u.id),
       'EVENT',
       'Evento de Mario Kart',
       'Torneo de Mario Kart abierto a toda la comunidad. Traé tu control, habrá premios y buena música.',
       TIMESTAMP '2026-11-21 16:00:00',
       'Sala de Juegos, Edificio C',
       CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM users u
WHERE u.id = (SELECT MIN(id) FROM users)
  AND NOT EXISTS (SELECT 1 FROM projects p WHERE p.title = 'Evento de Mario Kart');

-- Hackaton 2027
INSERT INTO projects (creator_id, kind, title, description, event_date, location, created_at, updated_at)
SELECT COALESCE((SELECT id FROM users ORDER BY id LIMIT 1 OFFSET 3), u.id),
       'EVENT',
       'Hackaton 2027',
       '48 horas de código, equipos y mentores para resolver retos del campus. Inscripciones abiertas.',
       TIMESTAMP '2027-03-13 09:00:00',
       'Aula Magna, Edificio Central',
       CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM users u
WHERE u.id = (SELECT MIN(id) FROM users)
  AND NOT EXISTS (SELECT 1 FROM projects p WHERE p.title = 'Hackaton 2027');

-- Reparación de techo del C
INSERT INTO projects (creator_id, kind, title, description, status, created_at, updated_at)
SELECT COALESCE((SELECT id FROM users ORDER BY id LIMIT 1 OFFSET 4), u.id),
       'PROJECT',
       'Reparacion de techo del C',
       'Reparar las filtraciones del techo del Edificio C para cuidar los espacios de estudio y el equipamiento.',
       'IN_PROGRESS', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM users u
WHERE u.id = (SELECT MIN(id) FROM users)
  AND NOT EXISTS (SELECT 1 FROM projects p WHERE p.title = 'Reparacion de techo del C');
