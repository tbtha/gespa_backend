package com.tbtha.gespa_backend.unit;

import com.tbtha.gespa_backend.auth.AuthService;
import com.tbtha.gespa_backend.dtos.PasswordResetConfirmRequest;
import com.tbtha.gespa_backend.dtos.PasswordResetRequest;
import com.tbtha.gespa_backend.entities.PasswordResetToken;
import com.tbtha.gespa_backend.entities.Usuario;
import com.tbtha.gespa_backend.entities.enums.UserRole;
import com.tbtha.gespa_backend.exceptions.ConflictException;
import com.tbtha.gespa_backend.repositories.PacienteRepository;
import com.tbtha.gespa_backend.repositories.PasswordResetTokenRepository;
import com.tbtha.gespa_backend.repositories.ProfessionalInvitationTokenRepository;
import com.tbtha.gespa_backend.repositories.ProfesionalRepository;
import com.tbtha.gespa_backend.repositories.RefreshTokenRepository;
import com.tbtha.gespa_backend.repositories.UsuarioRepository;
import com.tbtha.gespa_backend.security.AccessControlService;
import com.tbtha.gespa_backend.security.JwtService;
import com.tbtha.gespa_backend.services.AuditService;
import com.tbtha.gespa_backend.services.email.EmailService;
import com.tbtha.gespa_backend.services.email.UserAccountEmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * AU-10, AU-11, AU-12 del plan de pruebas — recuperación de contraseña.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock AuthenticationManager authenticationManager;
    @Mock UsuarioRepository usuarioRepository;
    @Mock PacienteRepository pacienteRepository;
    @Mock ProfesionalRepository profesionalRepository;
    @Mock PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock ProfessionalInvitationTokenRepository invitationTokenRepository;
    @Mock RefreshTokenRepository refreshTokenRepository;
    @Mock JwtService jwtService;
    @Mock AccessControlService accessControlService;
    @Mock AuditService auditService;
    @Mock EmailService emailService;
    @Mock UserAccountEmailService userAccountEmailService;
    @Mock PasswordEncoder passwordEncoder;

    AuthService service;

    @BeforeEach
    void setUp() {
        service = new AuthService(
                authenticationManager, usuarioRepository, pacienteRepository,
                profesionalRepository, passwordResetTokenRepository, invitationTokenRepository,
                refreshTokenRepository, jwtService, accessControlService, auditService,
                emailService, userAccountEmailService, passwordEncoder,
                86400L, 3600L,
                "http://localhost:5173/reset-password",
                true,   // exposeToken = true para verificar en tests
                10, 900L, 900L
        );
    }

    /**
     * AU-10 — Solicitar recuperación de contraseña con email válido.
     * Funcionalidad: POST /api/auth/password-reset/request con un email de
     * usuario activo debe generar un token de reset, enviarlo por correo
     * y retornar el token en la respuesta (solo cuando exposeToken=true,
     * configurado así en tests para verificación).
     */
    @Test
    @DisplayName("AU-10: requestPasswordReset con email válido genera token y envía correo")
    void requestPasswordReset_emailValido_generaTokenYEnviaCorreo() {
        Usuario user = buildUsuario(1L, true);
        when(usuarioRepository.findByEmail("prof1@gespa.cl")).thenReturn(Optional.of(user));
        doNothing().when(passwordResetTokenRepository).deleteByUser_Id(1L);
        when(passwordResetTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(emailService).sendHtml(any(), any(), any());

        var response = service.requestPasswordReset(new PasswordResetRequest("prof1@gespa.cl"));

        assertThat(response.message()).contains("token de recuperación");
        assertThat(response.resetToken()).isNotNull(); // exposeToken=true
        verify(emailService).sendHtml(eq("prof1@gespa.cl"), anyString(), anyString());
    }

    /**
     * AU-10b — Solicitud de recuperación con email no registrado retorna mensaje genérico.
     * Funcionalidad: POST /api/auth/password-reset/request con un email que
     * no existe debe retornar el mismo mensaje genérico que si existiera,
     * evitando revelar si el email está o no registrado en el sistema (seguridad).
     */
    @Test
    @DisplayName("AU-10b: requestPasswordReset con email inexistente retorna mensaje genérico sin error")
    void requestPasswordReset_emailInexistente_retornaMensajeGenerico() {
        when(usuarioRepository.findByEmail("noexiste@gespa.cl")).thenReturn(Optional.empty());

        var response = service.requestPasswordReset(new PasswordResetRequest("noexiste@gespa.cl"));

        assertThat(response.message()).isNotBlank();
        assertThat(response.resetToken()).isNull();
        verify(emailService, never()).sendHtml(any(), any(), any());
    }

    /**
     * AU-10c — Usuario inactivo no recibe correo de recuperación.
     * Funcionalidad: POST /api/auth/password-reset/request con el email de
     * un usuario desactivado no debe generar token ni enviar correo,
     * retornando el mensaje genérico como si el email no existiera.
     */
    @Test
    @DisplayName("AU-10c: requestPasswordReset con usuario inactivo no envía correo")
    void requestPasswordReset_usuarioInactivo_noEnviaCorreo() {
        Usuario inactivo = buildUsuario(2L, false);
        when(usuarioRepository.findByEmail("inactivo@gespa.cl")).thenReturn(Optional.of(inactivo));

        var response = service.requestPasswordReset(new PasswordResetRequest("inactivo@gespa.cl"));

        assertThat(response.message()).isNotBlank();
        verify(passwordResetTokenRepository, never()).save(any());
    }

    /**
     * AU-11 — Confirmar nueva contraseña con token válido.
     * Funcionalidad: POST /api/auth/password-reset/confirm con un token
     * vigente y no usado debe actualizar la contraseña del usuario,
     * marcar el token como usado y revocar todos sus refresh tokens activos.
     */
    @Test
    @DisplayName("AU-11: confirmPasswordReset con token válido cambia la contraseña del usuario")
    void confirmPasswordReset_tokenValido_cambiaContrasena() {
        Usuario user = buildUsuario(1L, true);
        PasswordResetToken token = buildToken(user, false, OffsetDateTime.now().plusHours(1));

        when(passwordResetTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token));
        when(refreshTokenRepository.findAllByUser_IdAndRevokedFalse(1L)).thenReturn(java.util.Collections.emptyList());
        when(passwordEncoder.encode("NuevaClave1!")).thenReturn("$2a$10$nuevohash");

        service.confirmPasswordReset(new PasswordResetConfirmRequest("token-valido", "NuevaClave1!"));

        assertThat(token.isUsed()).isTrue();
        assertThat(token.getUsedAt()).isNotNull();
        verify(passwordEncoder).encode("NuevaClave1!");
    }

    /**
     * AU-12 — Token de recuperación expirado es rechazado.
     * Funcionalidad: POST /api/auth/password-reset/confirm con un token
     * cuya fecha de expiración ya pasó debe lanzar ConflictException
     * sin modificar la contraseña del usuario.
     */
    @Test
    @DisplayName("AU-12: confirmPasswordReset con token expirado lanza ConflictException")
    void confirmPasswordReset_tokenExpirado_lanzaConflict() {
        Usuario user = buildUsuario(1L, true);
        PasswordResetToken token = buildToken(user, false, OffsetDateTime.now().minusHours(1)); // expirado

        when(passwordResetTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token));

        assertThatThrownBy(() ->
                service.confirmPasswordReset(new PasswordResetConfirmRequest("token-expirado", "Clave123!"))
        ).isInstanceOf(ConflictException.class)
         .hasMessageContaining("expirado");
    }

    /**
     * SEC-05 — Token de reset de un solo uso: el segundo intento es rechazado.
     * Funcionalidad: POST /api/auth/password-reset/confirm con un token que
     * ya fue utilizado previamente debe lanzar ConflictException, impidiendo
     * que un token interceptado pueda usarse más de una vez.
     */
    @Test
    @DisplayName("SEC-05: usar el mismo token de reset dos veces lanza ConflictException")
    void confirmPasswordReset_tokenYaUsado_lanzaConflict() {
        Usuario user = buildUsuario(1L, true);
        PasswordResetToken token = buildToken(user, true, OffsetDateTime.now().plusHours(1)); // usado=true

        when(passwordResetTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token));

        assertThatThrownBy(() ->
                service.confirmPasswordReset(new PasswordResetConfirmRequest("token-usado", "Clave123!"))
        ).isInstanceOf(ConflictException.class);
    }

    /**
     * AU-09 — Bloqueo de cuenta por intentos de login fallidos consecutivos.
     * Funcionalidad: tras 10 intentos fallidos de login en el mismo email
     * dentro de la ventana de tiempo configurada, la cuenta queda bloqueada
     * temporalmente y el siguiente intento retorna ConflictException con
     * mensaje que indica el bloqueo (sin revelar la contraseña real).
     */
    @Test
    @DisplayName("AU-09: login bloqueado tras 10 intentos fallidos")
    void login_bloqueadoDespuesDe10IntentosFallidos() {
        var request = new com.tbtha.gespa_backend.dtos.LoginRequest("prof1@gespa.cl", "wrong");

        doThrow(new org.springframework.security.authentication.BadCredentialsException("bad"))
                .when(authenticationManager).authenticate(any());

        // 10 intentos fallidos
        for (int i = 0; i < 10; i++) {
            try {
                service.loginAsProfessional(request);
            } catch (ConflictException ignored) {
            }
        }

        // El 11° debe lanzar "bloqueado" — ya no llega al authenticationManager
        assertThatThrownBy(() -> service.loginAsProfessional(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("bloqueada");
    }

    // --- helpers ---

    private Usuario buildUsuario(Long id, boolean active) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setEmail("prof1@gespa.cl");
        u.setDisplayName("Profesional Test");
        u.setRole(UserRole.PROFESSIONAL);
        u.setActive(active);
        u.setPasswordHash("$2a$10$hash");
        return u;
    }

    private PasswordResetToken buildToken(Usuario user, boolean used, OffsetDateTime expiresAt) {
        PasswordResetToken t = new PasswordResetToken();
        t.setUser(user);
        t.setTokenHash("hashed-token");
        t.setUsed(used);
        t.setExpiresAt(expiresAt);
        return t;
    }
}
