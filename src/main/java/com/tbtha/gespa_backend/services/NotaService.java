package com.tbtha.gespa_backend.services;

import com.tbtha.gespa_backend.dtos.CreateNotaRequest;
import com.tbtha.gespa_backend.dtos.NotaResponse;
import com.tbtha.gespa_backend.dtos.UpdateNotaRequest;
import com.tbtha.gespa_backend.entities.Cita;
import com.tbtha.gespa_backend.entities.Nota;
import com.tbtha.gespa_backend.entities.Paciente;
import com.tbtha.gespa_backend.entities.Profesional;
import com.tbtha.gespa_backend.exceptions.ResourceNotFoundException;
import com.tbtha.gespa_backend.repositories.CitaRepository;
import com.tbtha.gespa_backend.repositories.NotaRepository;
import com.tbtha.gespa_backend.repositories.PacienteRepository;
import com.tbtha.gespa_backend.repositories.ProfesionalRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NotaService {

    private final NotaRepository notaRepository;
    private final PacienteRepository pacienteRepository;
    private final ProfesionalRepository profesionalRepository;
    private final CitaRepository citaRepository;

    public NotaService(NotaRepository notaRepository,
                       PacienteRepository pacienteRepository,
                       ProfesionalRepository profesionalRepository,
                       CitaRepository citaRepository) {
        this.notaRepository = notaRepository;
        this.pacienteRepository = pacienteRepository;
        this.profesionalRepository = profesionalRepository;
        this.citaRepository = citaRepository;
    }

    @Transactional
    public NotaResponse create(Long pacienteId, CreateNotaRequest request) {
        Paciente paciente = pacienteRepository.findById(pacienteId)
                .orElseThrow(() -> new ResourceNotFoundException("Paciente no encontrado"));
        Profesional profesional = profesionalRepository.findById(request.professionalId())
                .orElseThrow(() -> new ResourceNotFoundException("Profesional no encontrado"));

        Cita cita = null;
        if (request.appointmentId() != null) {
            cita = citaRepository.findById(request.appointmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Cita no encontrada"));
        }

        Nota nota = new Nota();
        nota.setPaciente(paciente);
        nota.setProfesional(profesional);
        nota.setCita(cita);
        nota.setNoteType(request.noteType());
        nota.setContent(request.content());
        nota.setIndicaciones(request.indicaciones());
        nota.setPlan(request.plan());
        nota.setIsPrivate(request.isPrivate() == null ? true : request.isPrivate());

        return toResponse(notaRepository.save(nota));
    }

    @Transactional(readOnly = true)
    public List<NotaResponse> findByPaciente(Long pacienteId) {
        if (!pacienteRepository.existsById(pacienteId)) {
            throw new ResourceNotFoundException("Paciente no encontrado");
        }
        return notaRepository.findByPacienteIdOrderByCreatedAtDesc(pacienteId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public NotaResponse findById(Long id) {
        Nota nota = notaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Nota no encontrada"));
        return toResponse(nota);
    }

    @Transactional
    public NotaResponse update(Long id, UpdateNotaRequest request) {
        Nota nota = notaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Nota no encontrada"));

        nota.setNoteType(request.noteType());
        nota.setContent(request.content());
        nota.setIndicaciones(request.indicaciones());
        nota.setPlan(request.plan());
        if (request.isPrivate() != null) {
            nota.setIsPrivate(request.isPrivate());
        }

        return toResponse(notaRepository.save(nota));
    }

    @Transactional
    public void delete(Long id) {
        if (!notaRepository.existsById(id)) {
            throw new ResourceNotFoundException("Nota no encontrada");
        }
        notaRepository.deleteById(id);
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
}
