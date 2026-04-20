package com.tbtha.gespa_backend.services;

import com.tbtha.gespa_backend.dtos.dashboard.DashboardCitaItemResponse;
import com.tbtha.gespa_backend.dtos.dashboard.DashboardKpisResponse;
import com.tbtha.gespa_backend.dtos.dashboard.DashboardProfesionalResponse;
import com.tbtha.gespa_backend.dtos.dashboard.DashboardSerieItemResponse;
import com.tbtha.gespa_backend.dtos.dashboard.DashboardTipoAtencionItemResponse;
import com.tbtha.gespa_backend.entities.Cita;
import com.tbtha.gespa_backend.entities.enums.AppointmentStatus;
import com.tbtha.gespa_backend.repositories.CitaRepository;
import com.tbtha.gespa_backend.repositories.PacienteRepository;
import com.tbtha.gespa_backend.security.AccessControlService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.IsoFields;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class DashboardService {

    private final CitaRepository citaRepository;
    private final PacienteRepository pacienteRepository;
    private final AccessControlService accessControlService;

    public DashboardService(CitaRepository citaRepository,
                            PacienteRepository pacienteRepository,
                            AccessControlService accessControlService) {
        this.citaRepository = citaRepository;
        this.pacienteRepository = pacienteRepository;
        this.accessControlService = accessControlService;
    }

    public DashboardProfesionalResponse getProfesionalDashboard(Long profesionalId, int semanas, int limitProximas) {
        accessControlService.assertCanAccessProfesional(profesionalId);

        List<Cita> citas = citaRepository.findByProfesionalIdOrderByStartsAtAsc(profesionalId);

        DashboardKpisResponse kpis = buildKpis(profesionalId, citas);
        List<DashboardCitaItemResponse> proximas = buildProximas(citas, Math.max(1, Math.min(limitProximas, 100)));
        List<DashboardSerieItemResponse> porSemana = buildCitasPorSemana(citas, Math.max(1, Math.min(semanas, 52)));
        List<DashboardTipoAtencionItemResponse> tipos = buildTiposAtencion(citas);
        List<DashboardSerieItemResponse> porAnio = buildCitasPorAnio(citas);

        return new DashboardProfesionalResponse(kpis, proximas, porSemana, tipos, porAnio);
    }

    private DashboardKpisResponse buildKpis(Long profesionalId, List<Cita> citas) {
        long pacientesActivos = pacienteRepository.findByProfesionalId(profesionalId).size();

        LocalDate hoy = LocalDate.now();
        long citasHoy = citas.stream()
                .filter(c -> c.getStatus() != AppointmentStatus.CANCELLED)
                .filter(c -> c.getStartsAt().toLocalDate().isEqual(hoy))
                .count();

        long completadas = citas.stream().filter(c -> c.getStatus() == AppointmentStatus.COMPLETED).count();
        long ausentismo = citas.stream().filter(c -> c.getStatus() == AppointmentStatus.NO_SHOW).count();
        long base = completadas + ausentismo;

        double pctCompletadas = base == 0 ? 0D : round2((completadas * 100D) / base);
        double pctAusentismo = base == 0 ? 0D : round2((ausentismo * 100D) / base);

        return new DashboardKpisResponse(pacientesActivos, citasHoy, pctCompletadas, pctAusentismo);
    }

    private List<DashboardCitaItemResponse> buildProximas(List<Cita> citas, int limit) {
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime until = now.plusDays(2);

        return citas.stream()
                .filter(c -> c.getStatus() == AppointmentStatus.SCHEDULED || c.getStatus() == AppointmentStatus.CONFIRMED)
                .filter(c -> !c.getStartsAt().isBefore(now) && c.getStartsAt().isBefore(until))
                .sorted(Comparator.comparing(Cita::getStartsAt))
                .limit(limit)
                .map(this::toDashboardCita)
                .toList();
    }

    private List<DashboardSerieItemResponse> buildCitasPorSemana(List<Cita> citas, int semanas) {
        LocalDate desde = LocalDate.now().minusWeeks(semanas - 1).with(java.time.DayOfWeek.MONDAY);

        Map<String, Long> grouped = citas.stream()
                .filter(c -> !c.getStartsAt().toLocalDate().isBefore(desde))
                .collect(java.util.stream.Collectors.groupingBy(
                        c -> {
                            LocalDate d = c.getStartsAt().toLocalDate();
                            int year = d.get(IsoFields.WEEK_BASED_YEAR);
                            int week = d.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
                            return year + "-W" + String.format("%02d", week);
                        },
                        LinkedHashMap::new,
                        java.util.stream.Collectors.counting()
                ));

        return grouped.entrySet().stream()
                .map(e -> new DashboardSerieItemResponse(e.getKey(), e.getValue()))
                .toList();
    }

    private List<DashboardTipoAtencionItemResponse> buildTiposAtencion(List<Cita> citas) {
        Map<String, Long> grouped = citas.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        c -> c.getTipoAtencion() == null ? "SIN_DEFINIR" : c.getTipoAtencion().name(),
                        LinkedHashMap::new,
                        java.util.stream.Collectors.counting()
                ));

        return grouped.entrySet().stream()
                .map(e -> new DashboardTipoAtencionItemResponse(e.getKey(), e.getValue()))
                .toList();
    }

    private List<DashboardSerieItemResponse> buildCitasPorAnio(List<Cita> citas) {
        Map<String, Long> grouped = citas.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        c -> String.valueOf(c.getStartsAt().getYear()),
                        LinkedHashMap::new,
                        java.util.stream.Collectors.counting()
                ));

        return grouped.entrySet().stream()
                .map(e -> new DashboardSerieItemResponse(e.getKey(), e.getValue()))
                .toList();
    }

    private DashboardCitaItemResponse toDashboardCita(Cita c) {
        return new DashboardCitaItemResponse(
                c.getId(),
                c.getPaciente().getId(),
                c.getPaciente().getUsuario().getDisplayName(),
                c.getStartsAt(),
                c.getEndsAt(),
                c.getStatus(),
                c.getTipoAtencion(),
                c.getModalidad()
        );
    }

    private double round2(double value) {
        return Math.round(value * 100D) / 100D;
    }
}
