package com.tbtha.gespa_backend.services;

import com.tbtha.gespa_backend.entities.RegistroAuditoria;
import com.tbtha.gespa_backend.entities.Usuario;
import com.tbtha.gespa_backend.repositories.RegistroAuditoriaRepository;
import com.tbtha.gespa_backend.repositories.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class AuditService {

    private final RegistroAuditoriaRepository registroAuditoriaRepository;
    private final UsuarioRepository usuarioRepository;

    public AuditService(RegistroAuditoriaRepository registroAuditoriaRepository,
                        UsuarioRepository usuarioRepository) {
        this.registroAuditoriaRepository = registroAuditoriaRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional
    public void register(String action, String entityName, Long entityId) {
        register(action, entityName, entityId, null);
    }

    @Transactional
    public void register(String action, String entityName, Long entityId, String details) {
        RegistroAuditoria registro = new RegistroAuditoria();
        registro.setAction(action);
        registro.setEntityName(entityName);
        registro.setEntityId(entityId);
        registro.setDetails(details);
        registro.setUser(resolveCurrentUser().orElse(null));
        registroAuditoriaRepository.save(registro);
    }

    private Optional<Usuario> resolveCurrentUser() {
        try {
            org.springframework.security.core.Authentication auth =
                    org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || auth.getName() == null) {
                return Optional.empty();
            }
            return usuarioRepository.findByEmail(auth.getName());
        } catch (Exception ex) {
            return Optional.empty();
        }
    }
}
