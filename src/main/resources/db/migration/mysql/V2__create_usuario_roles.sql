-- MySQL
-- Prepara estructura para soportar múltiples roles por usuario sin romper compatibilidad actual.

CREATE TABLE IF NOT EXISTS usuario_roles (
    usuario_id BIGINT NOT NULL,
    role VARCHAR(20) NOT NULL,
    PRIMARY KEY (usuario_id, role),
    CONSTRAINT fk_usuario_roles_usuario
        FOREIGN KEY (usuario_id)
        REFERENCES usuarios(id)
        ON DELETE CASCADE
) ENGINE=InnoDB;

INSERT IGNORE INTO usuario_roles (usuario_id, role)
SELECT u.id, u.role
FROM usuarios u
WHERE u.role IS NOT NULL;
