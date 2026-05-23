-- PostgreSQL
-- Elimina el campo obsoleto license_number de profesionales.
ALTER TABLE profesionales
DROP COLUMN IF EXISTS license_number;
