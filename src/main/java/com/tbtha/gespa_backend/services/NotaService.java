package com.tbtha.gespa_backend.services;

import com.tbtha.gespa_backend.dtos.CreateNotaRequest;
import com.tbtha.gespa_backend.dtos.NotaResponse;
import com.tbtha.gespa_backend.dtos.UpdateNotaRequest;
import com.tbtha.gespa_backend.entities.Cita;
import com.tbtha.gespa_backend.entities.Nota;
import com.tbtha.gespa_backend.entities.Paciente;
import com.tbtha.gespa_backend.entities.Profesional;
import com.tbtha.gespa_backend.entities.Usuario;
import com.tbtha.gespa_backend.entities.enums.UserRole;
import com.tbtha.gespa_backend.exceptions.ConflictException;
import com.tbtha.gespa_backend.exceptions.ResourceNotFoundException;
import com.tbtha.gespa_backend.repositories.CitaRepository;
import com.tbtha.gespa_backend.repositories.NotaRepository;
import com.tbtha.gespa_backend.repositories.PacienteRepository;
import com.tbtha.gespa_backend.repositories.ProfesionalRepository;
import com.tbtha.gespa_backend.security.AccessControlService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NotaService {

    private final NotaRepository notaRepository;
    private final PacienteRepository pacienteRepository;
    private final ProfesionalRepository profesionalRepository;
    private final CitaRepository citaRepository;
    private final AccessControlService accessControlService;
    private final AuditService auditService;

    public NotaService(NotaRepository notaRepository,
                       PacienteRepository pacienteRepository,
                       ProfesionalRepository profesionalRepository,
                       CitaRepository citaRepository,
                       AccessControlService accessControlService,
                       AuditService auditService) {
        this.notaRepository = notaRepository;
        this.pacienteRepository = pacienteRepository;
        this.profesionalRepository = profesionalRepository;
        this.citaRepository = citaRepository;
        this.accessControlService = accessControlService;
        this.auditService = auditService;
    }

    @Transactional
    public NotaResponse create(Long pacienteId, CreateNotaRequest request) {
        Paciente paciente = pacienteRepository.findById(pacienteId)
                .orElseThrow(() -> new ResourceNotFoundException("Paciente no encontrado"));
        Profesional profesional = resolveProfessionalForWrite(request.professionalId());

        Cita cita = citaRepository.findById(request.appointmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Cita no encontrada"));
        if (!cita.getProfesional().getId().equals(profesional.getId())) {
            throw new ConflictException("La cita no pertenece al profesional autenticado");
        }
        if (!cita.getPaciente().getId().equals(paciente.getId())) {
            throw new ConflictException("La cita no pertenece al paciente indicado");
        }

        Nota nota = new Nota();
        nota.setPaciente(paciente);
        nota.setProfesional(profesional);
        nota.setCita(cita);
        nota.setNoteType(cita.getTipoAtencion() == null ? request.noteType() : cita.getTipoAtencion().name());
        nota.setContent(request.content());
        nota.setIndicaciones(request.indicaciones());
        nota.setPlan(request.plan());
        nota.setIsPrivate(request.isPrivate() == null ? true : request.isPrivate());

        Nota saved = notaRepository.save(nota);
        auditService.register("CREATE_NOTE", "notas", saved.getId(), "pacienteId=" + pacienteId);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<NotaResponse> findByPaciente(Long pacienteId) {
        accessControlService.assertCanAccessPaciente(pacienteId);
        if (!pacienteRepository.existsById(pacienteId)) {
            throw new ResourceNotFoundException("Paciente no encontrado");
        }
        Usuario actor = accessControlService.currentUsuario();
        List<Nota> notas = actor.getRole() == UserRole.ADMIN
                ? notaRepository.findByPacienteIdOrderByCreatedAtDesc(pacienteId)
                : notaRepository.findByPacienteIdAndProfesionalIdOrderByCreatedAtDesc(pacienteId, resolveCurrentProfessionalId());

        return notas
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public NotaResponse findById(Long id) {
        Nota nota = findAccessibleNota(id);
        return toResponse(nota);
    }

    @Transactional
    public NotaResponse update(Long id, UpdateNotaRequest request) {
        Nota nota = findAccessibleNota(id);

        nota.setNoteType(request.noteType());
        nota.setContent(request.content());
        nota.setIndicaciones(request.indicaciones());
        nota.setPlan(request.plan());
        if (request.isPrivate() != null) {
            nota.setIsPrivate(request.isPrivate());
        }

        Nota saved = notaRepository.save(nota);
        auditService.register("UPDATE_NOTE", "notas", saved.getId(), null);
        return toResponse(saved);
    }

    @Transactional
    public void delete(Long id) {
        Nota nota = findAccessibleNota(id);
        auditService.register("DELETE_NOTE", "notas", nota.getId(), null);
        notaRepository.delete(nota);
    }

    private NotaResponse toResponse(Nota nota) {
        return new NotaResponse(
                nota.getId(),
                nota.getPaciente().getId(),
                nota.getProfesional().getId(),
                nota.getCita() == null ? null : nota.getCita().getId(),
                nota.getNoteType(),
                nota.getContent(),
                nota.getIndicaciones(),
                nota.getPlan(),
                nota.getIsPrivate(),
                nota.getCreatedAt(),
                nota.getUpdatedAt()
        );
    }

    private Nota findAccessibleNota(Long id) {
        Usuario actor = accessControlService.currentUsuario();
        if (actor.getRole() == UserRole.ADMIN) {
            return notaRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Nota no encontrada"));
        }

        Long professionalId = resolveCurrentProfessionalId();
        return notaRepository.findByIdAndProfesionalId(id, professionalId)
                .orElseThrow(() -> new ResourceNotFoundException("Nota no encontrada"));
    }

    private Profesional resolveProfessionalForWrite(Long requestedProfessionalId) {
        Usuario actor = accessControlService.currentUsuario();
        Long professionalId;

        if (actor.getRole() == UserRole.PROFESSIONAL) {
            professionalId = actor.getId();
        } else if (actor.getRole() == UserRole.ADMIN) {
            professionalId = requestedProfessionalId;
        } else {
            throw new org.springframework.security.access.AccessDeniedException("No tienes permisos para registrar notas clínicas");
        }

        if (professionalId == null) {
            throw new ConflictException("Debe indicarse el profesional de la nota");
        }

        return profesionalRepository.findById(professionalId)
                .orElseThrow(() -> new ResourceNotFoundException("Profesional no encontrado"));
    }

    private Long resolveCurrentProfessionalId() {
        Usuario actor = accessControlService.currentUsuario();
        if (actor.getRole() != UserRole.PROFESSIONAL) {
            throw new org.springframework.security.access.AccessDeniedException("Solo el profesional autenticado puede acceder a notas clínicas");
        }
        return actor.getId();
    }
}
