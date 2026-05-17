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

    public void assertCanAccessProfesional(Long profesionalId) {
        Usuario actor = currentUsuario();
        if (actor.getRole() == UserRole.ADMIN) {
            return;
        }
        if (actor.getRole() == UserRole.PROFESSIONAL && actor.getId().equals(profesionalId)) {
            return;
        }
        throw new AccessDeniedException("No tienes permisos para acceder a este profesional");
    }

    public void assertCanAccessPaciente(Long pacienteId) {
        Usuario actor = currentUsuario();
        if (actor.getRole() == UserRole.ADMIN) {
            return;
        }
        if (actor.getRole() == UserRole.PATIENT && actor.getId().equals(pacienteId)) {
            return;
        }

        if (actor.getRole() == UserRole.PROFESSIONAL) {
            return;
        }

        throw new AccessDeniedException("No tienes permisos para acceder a este paciente");
    }

    public void assertCanAccessCita(Cita cita) {
        Usuario actor = currentUsuario();
        if (actor.getRole() == UserRole.ADMIN) {
            return;
        }
        if (actor.getRole() == UserRole.PROFESSIONAL && cita.getProfesional().getId().equals(actor.getId())) {
            return;
        }
        if (actor.getRole() == UserRole.PATIENT && cita.getPaciente().getId().equals(actor.getId())) {
            return;
        }
        throw new AccessDeniedException("No tienes permisos para acceder a esta cita");
    }
}
