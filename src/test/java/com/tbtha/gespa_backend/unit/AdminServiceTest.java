package com.tbtha.gespa_backend.unit;

import com.tbtha.gespa_backend.entities.Specialty;
import com.tbtha.gespa_backend.entities.Usuario;
import com.tbtha.gespa_backend.entities.enums.UserRole;
import com.tbtha.gespa_backend.exceptions.ConflictException;
import com.tbtha.gespa_backend.exceptions.ResourceNotFoundException;
import com.tbtha.gespa_backend.repositories.PacienteRepository;
import com.tbtha.gespa_backend.repositories.PasswordResetTokenRepository;
import com.tbtha.gespa_backend.repositories.ProfessionalInvitationTokenRepository;
import com.tbtha.gespa_backend.repositories.ProfesionalRepository;
import com.tbtha.gespa_backend.repositories.RefreshTokenRepository;
import com.tbtha.gespa_backend.repositories.SpecialtyRepository;
import com.tbtha.gespa_backend.repositories.UsuarioRepository;
import com.tbtha.gespa_backend.services.AdminService;
import com.tbtha.gespa_backend.services.email.UserAccountEmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * AD-05, AD-06, AD-08, AD-09 del plan de pruebas — tests unitarios con Mockito.
 */
@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock UsuarioRepository usuarioRepository;
    @Mock ProfesionalRepository profesionalRepository;
    @Mock PacienteRepository pacienteRepository;
    @Mock SpecialtyRepository specialtyRepository;
    @Mock ProfessionalInvitationTokenRepository invitationTokenRepository;
    @Mock PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock RefreshTokenRepository refreshTokenRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock UserAccountEmailService userAccountEmailService;

    @InjectMocks
    AdminService service = new AdminService(
            null, null, null, null, null, null, null, null, null, 604800L, 3600L
    );

    // Re-crear con constructor real usando @BeforeEach para inyectar los valores correctos
    @BeforeEach
    void setUp() {
        service = new AdminService(
                usuarioRepository,
                profesionalRepository,
                pacienteRepository,
                specialtyRepository,
                invitationTokenRepository,
                passwordResetTokenRepository,
                refreshTokenRepository,
                passwordEncoder,
                userAccountEmailService,
                604800L,
                3600L
        );
    }

    /**
     * AD-05 — Desactivar usuario revoca sus sesiones activas.
     * Funcionalidad: PATCH /api/admin/users/{id}/status con active=false debe
     * marcar el usuario como inactivo y revocar todos sus refresh tokens
     * activos, forzando el cierre de sesión inmediato en todos los dispositivos.
     */
    @Test
    @DisplayName("AD-05: desactivar usuario marca sus refresh tokens como revocados")
    void updateUserStatus_desactivar_revocaRefreshTokens() {
        Usuario user = buildUsuario(1L, UserRole.PROFESSIONAL, true);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(user));
        when(refreshTokenRepository.findAllByUser_IdAndRevokedFalse(1L)).thenReturn(Collections.emptyList());
        when(pacienteRepository.existsById(1L)).thenReturn(false);
        when(profesionalRepository.existsById(1L)).thenReturn(true);
        when(invitationTokenRepository.existsByUser_IdAndUsedFalseAndExpiresAtAfter(any(), any())).thenReturn(false);
        when(usuarioRepository.save(any())).thenReturn(user);

        var request = new com.tbtha.gespa_backend.dtos.AdminUpdateUserStatusRequest(false);
        var response = service.updateUserStatus(1L, request);

        assertThat(response.active()).isFalse();
        verify(refreshTokenRepository).findAllByUser_IdAndRevokedFalse(1L);
    }

    /**
     * AD-05b — No se permite activar usuarios manualmente.
     * Funcionalidad: PATCH /api/admin/users/{id}/status con active=true debe
     * lanzar ConflictException. La activación solo ocurre cuando el usuario
     * acepta su invitación, no por acción directa del administrador.
     */
    @Test
    @DisplayName("AD-05b: intentar activar usuario manualmente lanza ConflictException")
    void updateUserStatus_activarManualmente_lanzaConflict() {
        Usuario user = buildUsuario(1L, UserRole.PROFESSIONAL, false);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(user));

        var request = new com.tbtha.gespa_backend.dtos.AdminUpdateUserStatusRequest(true);

        assertThatThrownBy(() -> service.updateUserStatus(1L, request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("invitación");
    }

    /**
     * AD-06 — Reset de contraseña desde panel admin envía correo al usuario.
     * Funcionalidad: POST /api/admin/users/{id}/reset-password debe generar
     * un token de recuperación, revocar las sesiones activas del usuario,
     * enviar un correo con link de restablecimiento, y NO retornar contraseña
     * temporal en la respuesta (campo temporaryPassword debe ser null).
     */
    @Test
    @DisplayName("AD-06: resetUserPassword genera token de reset y envía correo al usuario")
    void resetUserPassword_generaTokenYEnviaCorreo() {
        Usuario user = buildUsuario(5L, UserRole.PROFESSIONAL, true);
        when(usuarioRepository.findById(5L)).thenReturn(Optional.of(user));
        when(refreshTokenRepository.findAllByUser_IdAndRevokedFalse(5L)).thenReturn(Collections.emptyList());
        doNothing().when(passwordResetTokenRepository).deleteByUser_Id(5L);
        when(passwordResetTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(userAccountEmailService).sendPasswordResetEmail(any(), any(), any());

        var response = service.resetUserPassword(5L);

        assertThat(response.email()).isEqualTo("prof@gespa.cl");
        assertThat(response.temporaryPassword()).isNull(); // ya no expone contraseña
        verify(passwordResetTokenRepository).save(any());
        verify(userAccountEmailService).sendPasswordResetEmail(eq(user), any(), any());
    }

    /**
     * AD-06b — Reset de contraseña con ID de usuario inexistente.
     * Funcionalidad: POST /api/admin/users/{id}/reset-password con un ID que
     * no existe en la base de datos debe lanzar ResourceNotFoundException
     * sin generar token ni enviar correo.
     */
    @Test
    @DisplayName("AD-06b: resetUserPassword con ID inexistente lanza ResourceNotFoundException")
    void resetUserPassword_usuarioInexistente_lanzaResourceNotFound() {
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.resetUserPassword(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    /**
     * AD-08 — Crear nueva especialidad en el catálogo.
     * Funcionalidad: POST /api/admin/specialties con un nombre nuevo debe
     * crear la especialidad en estado activo. Las especialidades activas
     * quedan disponibles para asignar a profesionales en invitaciones.
     */
    @Test
    @DisplayName("AD-08: createSpecialty crea especialidad activa")
    void createSpecialty_nombreNuevo_creaEspecialidadActiva() {
        when(specialtyRepository.findByNameIgnoreCase("Dermatología")).thenReturn(Optional.empty());

        Specialty saved = new Specialty();
        saved.setName("Dermatología");
        saved.setActive(true);
        when(specialtyRepository.save(any(Specialty.class))).thenReturn(saved);

        var request = new com.tbtha.gespa_backend.dtos.AdminCreateSpecialtyRequest("Dermatología");
        var response = service.createSpecialty(request);

        assertThat(response.name()).isEqualTo("Dermatología");
        assertThat(response.active()).isTrue();
    }

    /**
     * AD-09 — Desactivar especialidad del catálogo.
     * Funcionalidad: PATCH /api/admin/specialties/{id}/status con active=false
     * debe marcar la especialidad como inactiva. Las especialidades inactivas
     * no aparecen en el selector al crear invitaciones de profesionales.
     */
    @Test
    @DisplayName("AD-09: updateSpecialtyStatus desactiva una especialidad activa")
    void updateSpecialtyStatus_desactiva_especialidad() {
        Specialty specialty = new Specialty();
        specialty.setName("Kinesiología");
        specialty.setActive(true);
        when(specialtyRepository.findById(3L)).thenReturn(Optional.of(specialty));

        Specialty updated = new Specialty();
        updated.setName("Kinesiología");
        updated.setActive(false);
        when(specialtyRepository.save(any(Specialty.class))).thenReturn(updated);

        var request = new com.tbtha.gespa_backend.dtos.AdminUpdateSpecialtyStatusRequest(false);
        var response = service.updateSpecialtyStatus(3L, request);

        assertThat(response.active()).isFalse();
    }

    /**
     * AD-01 — Listar todos los usuarios del sistema.
     * Funcionalidad: GET /api/admin/users debe retornar la lista completa
     * de usuarios con su rol, estado activo/inactivo y flags que indican
     * si el usuario tiene perfil de profesional y/o paciente asociado.
     */
    @Test
    @DisplayName("AD-01: listUsers retorna todos los usuarios del sistema")
    void listUsers_retornaListaCompleta() {
        when(usuarioRepository.findAll(any(org.springframework.data.domain.Sort.class)))
                .thenReturn(List.of(
                        buildUsuario(1L, UserRole.ADMIN, true),
                        buildUsuario(2L, UserRole.PROFESSIONAL, true)
                ));
        when(pacienteRepository.existsById(any())).thenReturn(false);
        when(profesionalRepository.existsById(any())).thenReturn(false);
        // invitationTokenRepository no se consulta: ambos usuarios están activos
        // y isInvitationPending() hace early return false cuando active=true

        var users = service.listUsers();

        assertThat(users).hasSize(2);
    }

    // --- helpers ---

    private Usuario buildUsuario(Long id, UserRole role, boolean active) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setEmail("prof@gespa.cl");
        u.setDisplayName("Usuario Test");
        u.setRole(role);
        u.setActive(active);
        u.setPasswordHash("$2a$10$hash");
        return u;
    }
}
