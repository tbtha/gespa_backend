package com.tbtha.gespa_backend.services;

import org.springframework.stereotype.Service;

@Service
public class AuditService {

    public void register(String action, String entityName, Long entityId) {
        // TODO: Persistir en tabla registros_auditoria cuando se implementen entidades/repositorios.
    }
}
