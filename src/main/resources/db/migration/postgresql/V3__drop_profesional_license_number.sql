-- Archivo renombrado a V7__drop_profesional_license_number.sql para resolver conflicto de versiones de Flyway
-- El contenido original de la migración permanece igual
ALTER TABLE profesionales
DROP COLUMN IF EXISTS license_number;
DROP COLUMN IF EXISTS license_number;
