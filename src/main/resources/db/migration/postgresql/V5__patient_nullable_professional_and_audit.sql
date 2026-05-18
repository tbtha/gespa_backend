-- PostgreSQL
-- 1) Pacientes sin profesional asignado
-- 2) Antecedentes compartidos por paciente
-- 3) Tabla de auditoría

ALTER TABLE pacientes
    ALTER COLUMN professional_id DROP NOT NULL;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'uk_antecedente_patient_professional'
    ) THEN
        ALTER TABLE antecedentes DROP CONSTRAINT uk_antecedente_patient_professional;
    END IF;
END $$;

ALTER TABLE antecedentes
    ALTER COLUMN professional_id DROP NOT NULL;

DROP INDEX IF EXISTS idx_antecedente_patient_professional;
CREATE INDEX IF NOT EXISTS idx_antecedente_patient ON antecedentes(patient_id);

CREATE TABLE IF NOT EXISTS registros_auditoria (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NULL,
    entity_name VARCHAR(100),
    entity_id BIGINT,
    action VARCHAR(50) NOT NULL,
    details TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_registros_auditoria_user
        FOREIGN KEY (user_id)
        REFERENCES usuarios(id)
        ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_auditoria_user_created
    ON registros_auditoria(user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_auditoria_entity
    ON registros_auditoria(entity_name, entity_id, created_at DESC);
