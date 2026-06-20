package com.tbtha.gespa_backend.unit;

import com.tbtha.gespa_backend.dtos.CreateCitaRequest;
import com.tbtha.gespa_backend.entities.Cita;
import com.tbtha.gespa_backend.entities.Paciente;
import com.tbtha.gespa_backend.entities.Profesional;
import com.tbtha.gespa_backend.entities.Usuario;
import com.tbtha.gespa_backend.entities.enums.AppointmentStatus;
import com.tbtha.gespa_backend.entities.enums.ModalidadAtencion;
import com.tbtha.gespa_backend.entities.enums.TipoAtencion;
import com.tbtha.gespa_backend.entities.enums.UserRole;
import com.tbtha.gespa_backend.exceptions.ConflictException;
import com.tbtha.gespa_backend.exceptions.ResourceNotFoundException;
import com.tbtha.gespa_backend.repositories.CitaRepository;
import com.tbtha.gespa_backend.repositories.HorarioDisponibleRepository;
import com.tbtha.gespa_backend.repositories.PacienteRepository;
import com.tbtha.gespa_backend.repositories.ProfesionalRepository;
import com.tbtha.gespa_backend.security.AccessControlService;
import com.tbtha.gespa_backend.services.CitaService;
import com.tbtha.gespa_backend.services.email.AppointmentEmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * CIT-01, CIT-02, CIT-04, CIT-05 del plan de pruebas — tests unitarios con Mockito.
 */
@ExtendWith(MockitoExtension.class)
class CitaServiceTest {

    @Mock CitaRepository citaRepository;
    @Mock PacienteRepository pacienteRepository;
    @Mock ProfesionalRepository profesionalRepository;
    @Mock HorarioDisponibleRepository horarioDisponibleRepository;
    @Mock AccessControlService accessControlService;
    @Mock AppointmentEmailService appointmentEmailService;

    @InjectMocks
    CitaService service;

    private Paciente paciente;
    private Profesional profesional;
    private OffsetDateTime inicio;
    private OffsetDateTime fin;

    @BeforeEach
    void setUp() {
        Usuario uPac = new Usuario();
        uPac.setId(1L); uPac.setEmail("pac1@gespa.cl"); uPac.setDisplayName("Paciente Uno");
        uPac.setRole(UserRole.PATIENT); uPac.setActive(true);

        paciente = new Paciente();
        paciente.setUsuario(uPac);
        paciente.setRut("33333333-3");

        Usuario uProf = new Usuario();
        uProf.setId(2L); uProf.setEmail("prof1@gespa.cl"); uProf.setDisplayName("Dr. Profesional");
        uProf.setRole(UserRole.PROFESSIONAL); uProf.setActive(true);

        profesional = new Profesional();
        profesional.setUsuario(uProf);
        profesional.setRut("11111111-1");

        inicio = OffsetDateTime.now().plusDays(1).withHour(9).withMinute(0).withSecond(0).withNano(0);
        fin = inicio.plusMinutes(30);
    }

    /**
     * CIT-01 — Crear cita con datos válidos.
     * Funcionalidad: POST /api/citas con un paciente, profesional, rango horario
     * válido y tipo de atención debe crear la cita con status SCHEDULED y enviar
     * notificaciones por correo al paciente y al profesional.
     */
    @Test
    @DisplayName("CIT-01: crear cita con datos válidos retorna CitaResponse con status SCHEDULED")
    void create_datosValidos_retornaCitaScheduled() {
        CreateCitaRequest request = new CreateCitaRequest(
                1L, 2L, inicio, fin,
                TipoAtencion.PRIMERA_CONSULTA, ModalidadAtencion.PRESENCIAL, "Control inicial", null
        );

        when(citaRepository.existsByProfesionalIdAndStatusNotAndStartsAtLessThanAndEndsAtGreaterThan(
                any(), any(), any(), any())).thenReturn(false);
        when(pacienteRepository.findById(1L)).thenReturn(Optional.of(paciente));
        when(profesionalRepository.findById(2L)).thenReturn(Optional.of(profesional));
        when(horarioDisponibleRepository.findByProfesionalIdAndDiaSemanaAndActiveTrue(any(), anyInt()))
                .thenReturn(Collections.emptyList());

        Cita citaSaved = buildCita();
        when(citaRepository.save(any(Cita.class))).thenReturn(citaSaved);
        doNothing().when(appointmentEmailService).sendAppointmentNotifications(any());

        var response = service.create(request);

        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo(AppointmentStatus.SCHEDULED);
        assertThat(response.tipoAtencion()).isEqualTo(TipoAtencion.PRIMERA_CONSULTA);
        assertThat(response.modalidad()).isEqualTo(ModalidadAtencion.PRESENCIAL);
        verify(appointmentEmailService).sendAppointmentNotifications(any());
    }

    /**
     * CIT-02 — Validación: endsAt no puede ser igual o anterior a startsAt.
     * Funcionalidad: POST /api/citas con endsAt igual a startsAt debe rechazar
     * la solicitud con ConflictException antes de consultar la base de datos,
     * garantizando que no se persistan citas de duración cero o negativa.
     */
    @Test
    @DisplayName("CIT-02: crear cita con endsAt igual a startsAt lanza ConflictException")
    void create_endsAtIgualStartsAt_lanzaConflict() {
        CreateCitaRequest request = new CreateCitaRequest(
                1L, 2L, inicio, inicio,   // mismo instante
                TipoAtencion.CONTROL, ModalidadAtencion.PRESENCIAL, null, null
        );

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("finalizar");

        verify(citaRepository, never()).save(any());
    }

    /**
     * CIT-02b — Validación: paciente inexistente en creación de cita.
     * Funcionalidad: POST /api/citas con un pacienteId que no existe en la BD
     * debe lanzar ResourceNotFoundException con mensaje que identifique al
     * paciente como el recurso no encontrado.
     */
    @Test
    @DisplayName("CIT-02b: crear cita con pacienteId inexistente lanza ResourceNotFoundException")
    void create_pacienteInexistente_lanzaResourceNotFound() {
        CreateCitaRequest request = new CreateCitaRequest(
                99L, 2L, inicio, fin,
                TipoAtencion.CONTROL, ModalidadAtencion.PRESENCIAL, null, null
        );

        when(citaRepository.existsByProfesionalIdAndStatusNotAndStartsAtLessThanAndEndsAtGreaterThan(
                any(), any(), any(), any())).thenReturn(false);
        when(pacienteRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Paciente");
    }

    /**
     * CIT-04 — Cambiar estado de cita a COMPLETED.
     * Funcionalidad: PATCH /api/citas/{id}/estado con status=COMPLETED debe
     * actualizar el estado de la cita y retornar la cita actualizada.
     * Caso de uso: profesional marca una atención como completada.
     */
    @Test
    @DisplayName("CIT-04: updateEstado a COMPLETED actualiza el status de la cita")
    void updateEstado_aCompleted_actualizaStatus() {
        Cita cita = buildCita();
        when(citaRepository.findById(10L)).thenReturn(Optional.of(cita));
        doNothing().when(accessControlService).assertCanAccessCita(any());

        Cita completada = buildCita();
        completada.setStatus(AppointmentStatus.COMPLETED);
        when(citaRepository.save(any(Cita.class))).thenReturn(completada);

        var response = service.updateEstado(10L, AppointmentStatus.COMPLETED);

        assertThat(response.status()).isEqualTo(AppointmentStatus.COMPLETED);
    }

    /**
     * CIT-05 — Cambiar estado de cita a CANCELLED.
     * Funcionalidad: PATCH /api/citas/{id}/estado con status=CANCELLED debe
     * marcar la cita como cancelada. Las citas canceladas no bloquean el
     * horario del profesional para nuevas citas (no generan solapamiento).
     */
    @Test
    @DisplayName("CIT-05: updateEstado a CANCELLED actualiza el status correctamente")
    void updateEstado_aCancelled_actualizaStatus() {
        Cita cita = buildCita();
        when(citaRepository.findById(10L)).thenReturn(Optional.of(cita));
        doNothing().when(accessControlService).assertCanAccessCita(any());

        Cita cancelada = buildCita();
        cancelada.setStatus(AppointmentStatus.CANCELLED);
        when(citaRepository.save(any(Cita.class))).thenReturn(cancelada);

        var response = service.updateEstado(10L, AppointmentStatus.CANCELLED);

        assertThat(response.status()).isEqualTo(AppointmentStatus.CANCELLED);
    }

    /**
     * Extra — Solapamiento de horario impide crear la cita.
     * Funcionalidad: POST /api/citas cuando el profesional ya tiene una cita
     * activa (no cancelada) en el mismo rango horario debe lanzar
     * ConflictException sin persistir la nueva cita.
     */
    @Test
    @DisplayName("solapamiento de cita lanza ConflictException")
    void create_conSolapamiento_lanzaConflict() {
        CreateCitaRequest request = new CreateCitaRequest(
                1L, 2L, inicio, fin,
                TipoAtencion.CONTROL, ModalidadAtencion.PRESENCIAL, null, null
        );

        when(citaRepository.existsByProfesionalIdAndStatusNotAndStartsAtLessThanAndEndsAtGreaterThan(
                any(), any(), any(), any())).thenReturn(true);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("solapamiento");
    }

    // --- helpers ---

    private Cita buildCita() {
        Cita c = new Cita();
        c.setPaciente(paciente);
        c.setProfesional(profesional);
        c.setStartsAt(inicio);
        c.setEndsAt(fin);
        c.setStatus(AppointmentStatus.SCHEDULED);
        c.setTipoAtencion(TipoAtencion.PRIMERA_CONSULTA);
        c.setModalidad(ModalidadAtencion.PRESENCIAL);
        return c;
    }
}
