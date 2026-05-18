-- MySQL / MariaDB
-- 1) Pacientes sin profesional asignado
-- 2) Antecedentes compartidos por paciente
-- 3) Tabla de auditoría

ALTER TABLE pacientes
    MODIFY COLUMN professional_id BIGINT NULL;

ALTER TABLE antecedentes
    MODIFY COLUMN professional_id BIGINT NULL;

SET @drop_uk := (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.statistics
            WHERE table_schema = DATABASE()
              AND table_name = 'antecedentes'
              AND index_name = 'uk_antecedente_patient_professional'
        ),
        'ALTER TABLE antecedentes DROP INDEX uk_antecedente_patient_professional',
        'SELECT 1'
    )
);
PREPARE stmt1 FROM @drop_uk;
EXECUTE stmt1;
DEALLOCATE PREPARE stmt1;

SET @drop_idx := (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.statistics
            WHERE table_schema = DATABASE()
              AND table_name = 'antecedentes'
              AND index_name = 'idx_antecedente_patient_professional'
        ),
        'DROP INDEX idx_antecedente_patient_professional ON antecedentes',
        'SELECT 1'
    )
);
PREPARE stmt2 FROM @drop_idx;
EXECUTE stmt2;
DEALLOCATE PREPARE stmt2;

SET @create_idx := (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.statistics
            WHERE table_schema = DATABASE()
              AND table_name = 'antecedentes'
              AND index_name = 'idx_antecedente_patient'
        ),
        'SELECT 1',
        'CREATE INDEX idx_antecedente_patient ON antecedentes(patient_id)'
    )
);
PREPARE stmt3 FROM @create_idx;
EXECUTE stmt3;
DEALLOCATE PREPARE stmt3;

CREATE TABLE IF NOT EXISTS registros_auditoria (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NULL,
    entity_name VARCHAR(100) NULL,
    entity_id BIGINT NULL,
    action VARCHAR(50) NOT NULL,
    details TEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_registros_auditoria_user
        FOREIGN KEY (user_id)
        REFERENCES usuarios(id)
        ON DELETE SET NULL,
    INDEX idx_auditoria_user_created (user_id, created_at),
    INDEX idx_auditoria_entity (entity_name, entity_id, created_at)
) ENGINE=InnoDB;
