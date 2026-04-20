package com.tbtha.gespa_backend.security;

import com.tbtha.gespa_backend.entities.Cita;
import com.tbtha.gespa_backend.entities.Paciente;
import com.tbtha.gespa_backend.entities.Usuario;
import com.tbtha.gespa_backend.entities.enums.UserRole;
import com.tbtha.gespa_backend.exceptions.ResourceNotFoundException;
import com.tbtha.gespa_backend.repositories.PacienteRepository;
import com.tbtha.gespa_backend.repositories.UsuarioRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class AccessControlService {

    private final UsuarioRepository usuarioRepository;
    private final PacienteRepository pacienteRepository;

    public AccessControlService(UsuarioRepository usuarioRepository,
                                PacienteRepository pacienteRepository) {
        this.usuarioRepository = usuarioRepository;
        this.pacienteRepository = pacienteRepository;
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
            Paciente paciente = pacienteRepository.findById(pacienteId)
                    .orElseThrow(() -> new ResourceNotFoundException("Paciente no encontrado"));
            if (paciente.getProfesional().getId().equals(actor.getId())) {
                return;
            }
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
