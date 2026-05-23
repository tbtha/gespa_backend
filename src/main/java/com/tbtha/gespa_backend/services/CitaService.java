package com.tbtha.gespa_backend.services;

import com.tbtha.gespa_backend.dtos.CitaResponse;
import com.tbtha.gespa_backend.dtos.CreateCitaRequest;
import com.tbtha.gespa_backend.dtos.UpdateCitaRequest;
import com.tbtha.gespa_backend.entities.Cita;
import com.tbtha.gespa_backend.entities.Paciente;
import com.tbtha.gespa_backend.entities.Profesional;
import com.tbtha.gespa_backend.entities.enums.AppointmentStatus;
import com.tbtha.gespa_backend.entities.enums.ModalidadAtencion;
import com.tbtha.gespa_backend.exceptions.ConflictException;
import com.tbtha.gespa_backend.exceptions.ResourceNotFoundException;
import com.tbtha.gespa_backend.repositories.CitaRepository;
import com.tbtha.gespa_backend.repositories.PacienteRepository;
import com.tbtha.gespa_backend.repositories.ProfesionalRepository;
import com.tbtha.gespa_backend.repositories.HorarioDisponibleRepository;
import com.tbtha.gespa_backend.entities.HorarioDisponible;
import com.tbtha.gespa_backend.security.AccessControlService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.time.OffsetDateTime;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

@Service
public class CitaService {

    private final CitaRepository citaRepository;
    private final PacienteRepository pacienteRepository;
    private final ProfesionalRepository profesionalRepository;
    private final HorarioDisponibleRepository horarioDisponibleRepository;
    private final AccessControlService accessControlService;

    public CitaService(CitaRepository citaRepository,
                       PacienteRepository pacienteRepository,
                       ProfesionalRepository profesionalRepository,
                       AccessControlService accessControlService,
                       HorarioDisponibleRepository horarioDisponibleRepository) {
        this.citaRepository = citaRepository;
        this.pacienteRepository = pacienteRepository;
        this.profesionalRepository = profesionalRepository;
        this.accessControlService = accessControlService;
        this.horarioDisponibleRepository = horarioDisponibleRepository;
    }

    @Transactional
    public CitaResponse create(CreateCitaRequest request) {
        // Permitir que cualquier paciente agende con cualquier profesional

        if (!request.endsAt().isAfter(request.startsAt())) {
            throw new ConflictException("La cita no puede finalizar antes o al mismo tiempo que inicia");
        }

        validarSinSolapamiento(request.profesionalId(), request.startsAt(), request.endsAt(), null);

        Paciente paciente = pacienteRepository.findById(request.pacienteId())
                .orElseThrow(() -> new ResourceNotFoundException("Paciente no encontrado"));
        Profesional profesional = profesionalRepository.findById(request.profesionalId())
                .orElseThrow(() -> new ResourceNotFoundException("Profesional no encontrado"));

        Cita cita = new Cita();
        cita.setPaciente(paciente);
        cita.setProfesional(profesional);
        cita.setStartsAt(request.startsAt());
        cita.setEndsAt(request.endsAt());
        cita.setTipoAtencion(request.tipoAtencion());
        cita.setModalidad(request.modalidad() == null ? ModalidadAtencion.PRESENCIAL : request.modalidad());
        cita.setReason(request.reason());
        // Buscar el horario de atención correspondiente para obtener la dirección/lugar
        String lugar = request.location();
        try {
            int diaSemana = request.startsAt().getDayOfWeek().getValue();
            LocalTime horaInicio = request.startsAt().toLocalTime();
            List<HorarioDisponible> horarios = horarioDisponibleRepository.findByProfesionalIdAndDiaSemanaAndActiveTrue(request.profesionalId(), diaSemana);
            HorarioDisponible horarioMatch = horarios.stream()
                .filter(h -> !horaInicio.isBefore(h.getHoraInicio()) && horaInicio.isBefore(h.getHoraFin()))
                .findFirst().orElse(null);
            if (horarioMatch != null && horarioMatch.getDireccionAtencion() != null && !horarioMatch.getDireccionAtencion().isBlank()) {
                lugar = horarioMatch.getDireccionAtencion();
            }
        } catch (Exception e) {
            // Si falla, dejar el valor original
        }
        cita.setLocation(lugar);

        return toResponse(citaRepository.save(cita));
    }

    @Transactional(readOnly = true)
    public List<CitaResponse> findByProfesional(Long profesionalId) {
        // Permitir acceso universal a la agenda de profesionales
        return citaRepository.findByProfesionalIdOrderByStartsAtAsc(profesionalId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CitaResponse> findByPaciente(Long pacienteId) {
        accessControlService.assertCanAccessPaciente(pacienteId);
        return citaRepository.findByPacienteIdOrderByStartsAtAsc(pacienteId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CitaResponse> findAgendaByProfesional(Long profesionalId,
                                                      OffsetDateTime desde,
                                                      OffsetDateTime hasta) {
        // Permitir acceso universal a la agenda de profesionales

        if (desde == null || hasta == null) {
            return findByProfesional(profesionalId);
        }

        if (!hasta.isAfter(desde)) {
            throw new ConflictException("El rango de agenda es inválido: 'hasta' debe ser posterior a 'desde'");
        }

        return citaRepository.findByProfesionalIdAndStartsAtBetweenOrderByStartsAtAsc(profesionalId, desde, hasta)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public CitaResponse updateEstado(Long citaId, AppointmentStatus status) {
        Cita cita = citaRepository.findById(citaId)
                .orElseThrow(() -> new ResourceNotFoundException("Cita no encontrada"));
        accessControlService.assertCanAccessCita(cita);

        cita.setStatus(status);
        return toResponse(citaRepository.save(cita));
    }

    @Transactional
    public CitaResponse update(Long citaId, UpdateCitaRequest request) {
        if (!request.endsAt().isAfter(request.startsAt())) {
            throw new ConflictException("La cita no puede finalizar antes o al mismo tiempo que inicia");
        }

        Cita cita = citaRepository.findById(citaId)
                .orElseThrow(() -> new ResourceNotFoundException("Cita no encontrada"));
        accessControlService.assertCanAccessCita(cita);

        Paciente paciente = pacienteRepository.findById(request.pacienteId())
                .orElseThrow(() -> new ResourceNotFoundException("Paciente no encontrado"));
        Profesional profesional = profesionalRepository.findById(request.profesionalId())
                .orElseThrow(() -> new ResourceNotFoundException("Profesional no encontrado"));

        validarSinSolapamiento(request.profesionalId(), request.startsAt(), request.endsAt(), citaId);

        cita.setPaciente(paciente);
        cita.setProfesional(profesional);
        cita.setStartsAt(request.startsAt());
        cita.setEndsAt(request.endsAt());
        cita.setTipoAtencion(request.tipoAtencion());
        cita.setModalidad(request.modalidad() == null ? ModalidadAtencion.PRESENCIAL : request.modalidad());
        cita.setReason(request.reason());
        cita.setLocation(request.location());
        if (request.status() != null) {
            cita.setStatus(request.status());
        }

        return toResponse(citaRepository.save(cita));
    }

    private void validarSinSolapamiento(Long profesionalId,
                                        OffsetDateTime startsAt,
                                        OffsetDateTime endsAt,
                                        Long citaIdExcluir) {
        boolean existeSolapamiento;
        if (citaIdExcluir == null) {
            existeSolapamiento = citaRepository
                    .existsByProfesionalIdAndStatusNotAndStartsAtLessThanAndEndsAtGreaterThan(
                            profesionalId,
                            AppointmentStatus.CANCELLED,
                            endsAt,
                            startsAt
                    );
        } else {
            existeSolapamiento = citaRepository
                    .existsByProfesionalIdAndStatusNotAndStartsAtLessThanAndEndsAtGreaterThanAndIdNot(
                            profesionalId,
                            AppointmentStatus.CANCELLED,
                            endsAt,
                            startsAt,
                            citaIdExcluir
                    );
        }

        if (existeSolapamiento) {
            throw new ConflictException("Existe solapamiento de horario para el profesional en el rango indicado");
        }
    }

    private CitaResponse toResponse(Cita cita) {
        return new CitaResponse(
                cita.getId(),
                cita.getPaciente().getId(),
            cita.getPaciente().getUsuario().getDisplayName(),
                cita.getProfesional().getId(),
                cita.getProfesional().getUsuario().getDisplayName(),
                cita.getProfesional().getSpecialty(),
                cita.getStartsAt(),
                cita.getEndsAt(),
                cita.getStatus(),
                cita.getTipoAtencion(),
                cita.getModalidad(),
                cita.getReason(),
                cita.getLocation()
        );
    }
}
