package com.tbtha.gespa_backend.unit;

import com.tbtha.gespa_backend.dtos.AntecedentesResponse;
import com.tbtha.gespa_backend.dtos.UpsertAntecedentesRequest;
import com.tbtha.gespa_backend.entities.Antecedente;
import com.tbtha.gespa_backend.entities.Paciente;
import com.tbtha.gespa_backend.entities.Usuario;
import com.tbtha.gespa_backend.entities.enums.ActividadFisica;
import com.tbtha.gespa_backend.entities.enums.ConsumoNivel;
import com.tbtha.gespa_backend.entities.enums.ConsumoTabaco;
import com.tbtha.gespa_backend.entities.enums.UserRole;
import com.tbtha.gespa_backend.exceptions.ResourceNotFoundException;
import com.tbtha.gespa_backend.repositories.AntecedenteRepository;
import com.tbtha.gespa_backend.repositories.PacienteRepository;
import com.tbtha.gespa_backend.security.AccessControlService;
import com.tbtha.gespa_backend.services.AntecedenteService;
import com.tbtha.gespa_backend.services.AuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios del servicio AntecedenteService.
 * Módulo: Antecedentes Clínicos (ANT-01 a ANT-05 del plan de pruebas).
 * Tipo: Unitario con Mockito — no levanta Spring, no usa BD.
 */
@ExtendWith(MockitoExtension.class)
class AntecedenteServiceTest {

    @Mock AntecedenteRepository antecedenteRepository;
    @Mock PacienteRepository pacienteRepository;
    @Mock AccessControlService accessControlService;
    @Mock AuditService auditService;

    @InjectMocks
    AntecedenteService service;

    private Paciente paciente;
    private UpsertAntecedentesRequest requestValido;

    @BeforeEach
    void setUp() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setEmail("pac1@gespa.cl");
        usuario.setDisplayName("Paciente Uno");
        usuario.setRole(UserRole.PATIENT);
        usuario.setActive(true);

        paciente = new Paciente();
        paciente.setUsuario(usuario);
        paciente.setRut("33333333-3");

        requestValido = new UpsertAntecedentesRequest(
                "Hipertensión", null,
                ConsumoNivel.NO_CONSUME, ConsumoTabaco.NO_CONSUME, ConsumoNivel.NO_CONSUME,
                ActividadFisica.UNA_DOS_SEMANA,
                "Apendicectomía 2010", "Losartán 50mg", null
        );
    }

    /**
     * ANT-01 — Obtener antecedentes de paciente sin registros previos.
     * Funcionalidad: GET /api/pacientes/{id}/antecedentes cuando el paciente
     * no tiene antecedentes guardados debe retornar una respuesta vacía
     * (sin lanzar error), con id=null y todos los campos clínicos nulos.
     */
    @Test
    @DisplayName("ANT-01: findByPaciente devuelve respuesta vacía cuando no hay antecedentes")
    void findByPaciente_sinAntecedentes_retornaRespuestaVacia() {
        when(antecedenteRepository.findFirstByPacienteIdOrderByUpdatedAtDesc(1L))
                .thenReturn(Optional.empty());
        doNothing().when(accessControlService).assertCanAccessPaciente(1L);

        AntecedentesResponse response = service.findByPaciente(1L);

        assertThat(response.id()).isNull();
        assertThat(response.pacienteId()).isEqualTo(1L);
        assertThat(response.enfermedadesBase()).isNull();
    }

    /**
     * ANT-02 — Crear antecedentes clínicos por primera vez (upsert insert).
     * Funcionalidad: PUT /api/pacientes/{id}/antecedentes cuando no existe
     * registro previo debe crear uno nuevo con todos los campos del request,
     * llamar a save() y registrar la acción en auditoría.
     */
    @Test
    @DisplayName("ANT-02: upsert crea nuevos antecedentes cuando no existían")
    void upsert_creaAntecedentesNuevos() {
        when(accessControlService.currentUserRole()).thenReturn(UserRole.PROFESSIONAL);
        doNothing().when(accessControlService).assertCanAccessPaciente(1L);
        when(pacienteRepository.findById(1L)).thenReturn(Optional.of(paciente));
        when(antecedenteRepository.findFirstByPacienteIdOrderByUpdatedAtDesc(1L))
                .thenReturn(Optional.empty());

        Antecedente antecedenteSaved = buildAntecedente();
        when(antecedenteRepository.save(any(Antecedente.class))).thenReturn(antecedenteSaved);

        AntecedentesResponse response = service.upsert(1L, requestValido);

        assertThat(response.enfermedadesBase()).isEqualTo("Hipertensión");
        assertThat(response.actividadFisica()).isEqualTo(ActividadFisica.UNA_DOS_SEMANA);
        verify(antecedenteRepository).save(any(Antecedente.class));
        verify(auditService).register(eq("UPSERT_ANTECEDENTE"), eq("antecedentes"), any(), any());
    }

    /**
     * ANT-03 — Actualizar antecedentes existentes (upsert update).
     * Funcionalidad: PUT /api/pacientes/{id}/antecedentes cuando ya existe
     * un registro debe actualizar ese mismo registro (sin crear uno nuevo),
     * verificando que save() se llame exactamente una vez.
     */
    @Test
    @DisplayName("ANT-03: upsert actualiza antecedentes existentes sin crear duplicado")
    void upsert_actualizaAntecedentesExistentes() {
        when(accessControlService.currentUserRole()).thenReturn(UserRole.PROFESSIONAL);
        doNothing().when(accessControlService).assertCanAccessPaciente(1L);
        when(pacienteRepository.findById(1L)).thenReturn(Optional.of(paciente));

        Antecedente existente = buildAntecedente();
        existente.setEnfermedadesBase("Diabetes");
        when(antecedenteRepository.findFirstByPacienteIdOrderByUpdatedAtDesc(1L))
                .thenReturn(Optional.of(existente));

        UpsertAntecedentesRequest requestActualizado = new UpsertAntecedentesRequest(
                "Diabetes controlada", null,
                ConsumoNivel.OCASIONAL, ConsumoTabaco.EXFUMADOR, ConsumoNivel.NO_CONSUME,
                ActividadFisica.TRES_MAS_SEMANA, null, "Metformina", null
        );

        Antecedente updated = buildAntecedente();
        updated.setEnfermedadesBase("Diabetes controlada");
        when(antecedenteRepository.save(any(Antecedente.class))).thenReturn(updated);

        AntecedentesResponse response = service.upsert(1L, requestActualizado);

        assertThat(response.enfermedadesBase()).isEqualTo("Diabetes controlada");
        verify(antecedenteRepository, times(1)).save(any(Antecedente.class));
    }

    /**
     * ANT-04 — El rol PATIENT no puede editar antecedentes clínicos.
     * Funcionalidad: PUT /api/pacientes/{id}/antecedentes cuando el usuario
     * autenticado tiene rol PATIENT debe lanzar AccessDeniedException sin
     * llegar a persistir ningún dato en la base de datos.
     */
    @Test
    @DisplayName("ANT-04: paciente que intenta editar antecedentes recibe AccessDeniedException")
    void upsert_conRolPaciente_lanzaAccessDeniedException() {
        when(accessControlService.currentUserRole()).thenReturn(UserRole.PATIENT);
        doNothing().when(accessControlService).assertCanAccessPaciente(1L);

        assertThatThrownBy(() -> service.upsert(1L, requestValido))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("paciente no puede editar");

        verify(antecedenteRepository, never()).save(any());
    }

    /**
     * ANT-05 (Regresión) — El valor ActividadFisica.UNA_DOS_SEMANA es aceptado.
     * Funcionalidad: este test verifica que el bug corregido (el frontend enviaba
     * "MODERADO" en lugar de "UNA_DOS_SEMANA") no regrese. El enum debe aceptar
     * UNA_DOS_SEMANA y persistirlo correctamente sin error de deserialización.
     */
    @Test
    @DisplayName("ANT-05 (regresión): ActividadFisica.UNA_DOS_SEMANA se persiste correctamente")
    void upsert_actividadFisicaUnaDosSemana_sePersiste() {
        when(accessControlService.currentUserRole()).thenReturn(UserRole.PROFESSIONAL);
        doNothing().when(accessControlService).assertCanAccessPaciente(1L);
        when(pacienteRepository.findById(1L)).thenReturn(Optional.of(paciente));
        when(antecedenteRepository.findFirstByPacienteIdOrderByUpdatedAtDesc(1L))
                .thenReturn(Optional.empty());

        Antecedente saved = buildAntecedente();
        saved.setActividadFisica(ActividadFisica.UNA_DOS_SEMANA);
        when(antecedenteRepository.save(any(Antecedente.class))).thenReturn(saved);

        AntecedentesResponse response = service.upsert(1L, requestValido);

        assertThat(response.actividadFisica()).isEqualTo(ActividadFisica.UNA_DOS_SEMANA);
    }

    /**
     * Extra — Paciente inexistente lanza ResourceNotFoundException.
     * Funcionalidad: PUT /api/pacientes/{id}/antecedentes con un ID de paciente
     * que no existe en la base de datos debe lanzar ResourceNotFoundException
     * antes de intentar guardar cualquier dato.
     */
    @Test
    @DisplayName("upsert lanza ResourceNotFoundException si el paciente no existe")
    void upsert_pacienteInexistente_lanzaResourceNotFound() {
        when(accessControlService.currentUserRole()).thenReturn(UserRole.PROFESSIONAL);
        doNothing().when(accessControlService).assertCanAccessPaciente(99L);
        when(pacienteRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.upsert(99L, requestValido))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // --- helpers ---

    private Antecedente buildAntecedente() {
        Antecedente a = new Antecedente();
        a.setPaciente(paciente);
        a.setEnfermedadesBase("Hipertensión");
        a.setConsumoAlcohol(ConsumoNivel.NO_CONSUME);
        a.setConsumoTabaco(ConsumoTabaco.NO_CONSUME);
        a.setConsumoDrogas(ConsumoNivel.NO_CONSUME);
        a.setActividadFisica(ActividadFisica.UNA_DOS_SEMANA);
        a.setMedicamentosRegulares("Losartán 50mg");
        return a;
    }
}
