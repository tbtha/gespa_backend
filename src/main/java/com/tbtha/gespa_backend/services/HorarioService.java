package com.tbtha.gespa_backend.services;

import com.tbtha.gespa_backend.dtos.CreateHorarioRequest;
import com.tbtha.gespa_backend.dtos.HorarioDisponibleResponse;
import com.tbtha.gespa_backend.dtos.SlotDisponibleResponse;
import com.tbtha.gespa_backend.entities.HorarioDisponible;
import com.tbtha.gespa_backend.entities.Profesional;
import com.tbtha.gespa_backend.entities.enums.AppointmentStatus;
import com.tbtha.gespa_backend.entities.enums.ModalidadAtencion;
import com.tbtha.gespa_backend.exceptions.ResourceNotFoundException;
import com.tbtha.gespa_backend.repositories.CitaRepository;
import com.tbtha.gespa_backend.repositories.HorarioDisponibleRepository;
import com.tbtha.gespa_backend.repositories.ProfesionalRepository;
import com.tbtha.gespa_backend.security.AccessControlService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class HorarioService {

    private final HorarioDisponibleRepository horarioRepository;
    private final ProfesionalRepository profesionalRepository;
    private final CitaRepository citaRepository;
    private final AccessControlService accessControlService;

    public HorarioService(HorarioDisponibleRepository horarioRepository,
                          ProfesionalRepository profesionalRepository,
                          CitaRepository citaRepository,
                          AccessControlService accessControlService) {
        this.horarioRepository = horarioRepository;
        this.profesionalRepository = profesionalRepository;
        this.citaRepository = citaRepository;
        this.accessControlService = accessControlService;
    }

    @Transactional
    public HorarioDisponibleResponse create(Long profesionalId, CreateHorarioRequest request) {
        accessControlService.assertCanAccessProfesional(profesionalId);

        Profesional profesional = profesionalRepository.findById(profesionalId)
                .orElseThrow(() -> new ResourceNotFoundException("Profesional no encontrado"));

        LocalTime inicio = LocalTime.parse(request.horaInicio(), DateTimeFormatter.ofPattern("HH:mm"));
        LocalTime fin = LocalTime.parse(request.horaFin(), DateTimeFormatter.ofPattern("HH:mm"));

        if (!fin.isAfter(inicio)) {
            throw new IllegalArgumentException("La hora de fin debe ser posterior a la hora de inicio");
        }

        HorarioDisponible horario = new HorarioDisponible();
        horario.setProfesional(profesional);
        horario.setDiaSemana(request.diaSemana());
        horario.setHoraInicio(inicio);
        horario.setHoraFin(fin);
        horario.setDuracionMinutos(request.duracionMinutos() != null ? request.duracionMinutos() : 30);
        horario.setModalidad(ModalidadAtencion.valueOf(request.modalidad().toUpperCase()));

        return toResponse(horarioRepository.save(horario));
    }

    @Transactional(readOnly = true)
    public List<HorarioDisponibleResponse> listByProfesional(Long profesionalId) {
        if (!profesionalRepository.existsById(profesionalId)) {
            throw new ResourceNotFoundException("Profesional no encontrado");
        }
        return horarioRepository.findByProfesionalId(profesionalId).stream()
                .map(this::toResponse).toList();
    }

    @Transactional
    public void delete(Long profesionalId, Long horarioId) {
        accessControlService.assertCanAccessProfesional(profesionalId);
        HorarioDisponible horario = horarioRepository.findById(horarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Horario no encontrado"));
        if (!horario.getProfesional().getId().equals(profesionalId)) {
            throw new AccessDeniedException("No tienes permiso para eliminar este horario");
        }
        horarioRepository.delete(horario);
    }

    /**
     * Returns all available (unbooked) time slots for a professional on a given date.
     * The date is the client's local date; slots are generated in UTC-offset matching Chile (auto-detected via ZoneOffset.UTC for simplicity).
     * Frontend should pass the date as YYYY-MM-DD.
     */
    @Transactional(readOnly = true)
    public List<SlotDisponibleResponse> getSlots(Long profesionalId, String fechaStr) {
        Profesional profesional = profesionalRepository.findById(profesionalId)
                .orElseThrow(() -> new ResourceNotFoundException("Profesional no encontrado"));

        LocalDate fecha = LocalDate.parse(fechaStr);
        int diaSemana = fecha.getDayOfWeek().getValue(); // 1=MON … 7=SUN

        List<HorarioDisponible> horarios = horarioRepository
                .findByProfesionalIdAndDiaSemanaAndActiveTrue(profesionalId, diaSemana);

        List<SlotDisponibleResponse> slots = new ArrayList<>();

        for (HorarioDisponible horario : horarios) {
            LocalTime cursor = horario.getHoraInicio();
            LocalTime fin = horario.getHoraFin();
            int duracion = horario.getDuracionMinutos();

            while (!cursor.plusMinutes(duracion).isAfter(fin)) {
                LocalTime slotEnd = cursor.plusMinutes(duracion);

                OffsetDateTime startsAt = OffsetDateTime.of(fecha, cursor, ZoneOffset.UTC);
                OffsetDateTime endsAt = OffsetDateTime.of(fecha, slotEnd, ZoneOffset.UTC);

                boolean occupied = citaRepository
                        .existsByProfesionalIdAndStatusNotAndStartsAtLessThanAndEndsAtGreaterThan(
                                profesionalId,
                                AppointmentStatus.CANCELLED,
                                endsAt,
                                startsAt
                        );

                if (!occupied) {
                    slots.add(new SlotDisponibleResponse(
                            startsAt.toString(),
                            endsAt.toString(),
                            horario.getModalidad().name(),
                            profesional.getId(),
                            profesional.getUsuario().getDisplayName(),
                            profesional.getSpecialty()
                    ));
                }

                cursor = slotEnd;
            }
        }

        return slots;
    }

    /**
     * Returns available slots across all professionals for a given date (patient-facing).
     */
    @Transactional(readOnly = true)
    public List<SlotDisponibleResponse> getAllSlots(String fechaStr) {
        List<Profesional> todos = profesionalRepository.findAll();
        List<SlotDisponibleResponse> all = new ArrayList<>();
        for (Profesional p : todos) {
            all.addAll(getSlots(p.getId(), fechaStr));
        }
        return all;
    }

    private HorarioDisponibleResponse toResponse(HorarioDisponible h) {
        return new HorarioDisponibleResponse(
                h.getId(),
                h.getProfesional().getId(),
                h.getDiaSemana(),
                h.getHoraInicio().format(DateTimeFormatter.ofPattern("HH:mm")),
                h.getHoraFin().format(DateTimeFormatter.ofPattern("HH:mm")),
                h.getDuracionMinutos(),
                h.getModalidad().name(),
                h.isActive()
        );
    }
}
