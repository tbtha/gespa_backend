package com.tbtha.gespa_backend.security;

import com.tbtha.gespa_backend.entities.Cita;
import com.tbtha.gespa_backend.entities.Usuario;
import com.tbtha.gespa_backend.entities.enums.UserRole;
import com.tbtha.gespa_backend.repositories.UsuarioRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class AccessControlService {

    private final UsuarioRepository usuarioRepository;

    public AccessControlService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    public Usuario currentUsuario() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new AccessDeniedException("No autenticado");
        }

        return usuarioRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new AccessDeniedException("Usuario autenticado no existe"));
    }

    public UserRole currentUserRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AppUserPrincipal principal) {
            return principal.getRole();
        }
        return currentUsuario().getRole();
    }

    public void assertCanAccessProfesional(Long profesionalId) {
        Usuario actor = currentUsuario();
        UserRole actorRole = currentUserRole();
        if (actorRole == UserRole.ADMIN) {
            return;
        }
        if (actorRole == UserRole.PROFESSIONAL && actor.getId().equals(profesionalId)) {
            return;
        }
        throw new AccessDeniedException("No tienes permisos para acceder a este profesional");
    }

    public void assertCanAccessPaciente(Long pacienteId) {
        Usuario actor = currentUsuario();
        UserRole actorRole = currentUserRole();
        if (actorRole == UserRole.ADMIN) {
            return;
        }
        if (actorRole == UserRole.PATIENT && actor.getId().equals(pacienteId)) {
            return;
        }

        if (actorRole == UserRole.PROFESSIONAL) {
            return;
        }

        throw new AccessDeniedException("No tienes permisos para acceder a este paciente");
    }

    public void assertCanAccessCita(Cita cita) {
        Usuario actor = currentUsuario();
        UserRole actorRole = currentUserRole();
        if (actorRole == UserRole.ADMIN) {
            return;
        }
        if (actorRole == UserRole.PROFESSIONAL && cita.getProfesional().getId().equals(actor.getId())) {
            return;
        }
        if (actorRole == UserRole.PATIENT && cita.getPaciente().getId().equals(actor.getId())) {
            return;
        }
        throw new AccessDeniedException("No tienes permisos para acceder a esta cita");
    }
}
