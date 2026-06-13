package com.tbtha.gespa_backend.auth;

import com.tbtha.gespa_backend.dtos.CheckEmailResponse;
import com.tbtha.gespa_backend.dtos.LoginRequest;
import com.tbtha.gespa_backend.dtos.LoginResponse;
import com.tbtha.gespa_backend.dtos.MeResponse;
import com.tbtha.gespa_backend.dtos.AcceptProfessionalInvitationRequest;
import com.tbtha.gespa_backend.dtos.PasswordResetConfirmRequest;
import com.tbtha.gespa_backend.dtos.PasswordResetRequest;
import com.tbtha.gespa_backend.dtos.PasswordResetRequestResponse;
import com.tbtha.gespa_backend.dtos.RegisterPatientRequest;
import com.tbtha.gespa_backend.dtos.RegisterPatientResponse;
import com.tbtha.gespa_backend.entities.PasswordResetToken;
import com.tbtha.gespa_backend.entities.Paciente;
import com.tbtha.gespa_backend.entities.ProfessionalInvitationToken;
import com.tbtha.gespa_backend.entities.RefreshToken;
import com.tbtha.gespa_backend.entities.Usuario;
import com.tbtha.gespa_backend.entities.enums.UserRole;
import com.tbtha.gespa_backend.exceptions.ConflictException;
import com.tbtha.gespa_backend.exceptions.ResourceNotFoundException;
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
import com.tbtha.gespa_backend.utils.RutUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.net.URLEncoder;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UsuarioRepository usuarioRepository;
    private final PacienteRepository pacienteRepository;
    private final ProfesionalRepository profesionalRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final ProfessionalInvitationTokenRepository invitationTokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final AccessControlService accessControlService;
    private final AuditService auditService;
    private final EmailService emailService;
    private final UserAccountEmailService userAccountEmailService;
    private final PasswordEncoder passwordEncoder;
    private final long refreshExpirationSeconds;
    private final long passwordResetExpirationSeconds;
    private final String passwordResetFrontendUrl;
    private final boolean exposePasswordResetToken;
    private final int loginMaxFailedAttempts;
    private final long loginFailedWindowSeconds;
    private final long loginLockSeconds;
    private final ConcurrentMap<String, FailedLoginAttempt> failedLogins = new ConcurrentHashMap<>();

    public AuthService(AuthenticationManager authenticationManager,
                       UsuarioRepository usuarioRepository,
                       PacienteRepository pacienteRepository,
                       ProfesionalRepository profesionalRepository,
                       PasswordResetTokenRepository passwordResetTokenRepository,
                       ProfessionalInvitationTokenRepository invitationTokenRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       JwtService jwtService,
                       AccessControlService accessControlService,
                       AuditService auditService,
                       EmailService emailService,
                       UserAccountEmailService userAccountEmailService,
                       PasswordEncoder passwordEncoder,
                       @Value("${app.jwt.refresh-expiration-seconds:1209600}") long refreshExpirationSeconds,
                       @Value("${app.auth.password-reset.expiration-seconds:3600}") long passwordResetExpirationSeconds,
                       @Value("${app.auth.password-reset.frontend-url:http://localhost:5173/reset-password}") String passwordResetFrontendUrl,
                       @Value("${app.auth.password-reset.expose-token:false}") boolean exposePasswordResetToken,
                       @Value("${app.auth.login.max-failed-attempts:10}") int loginMaxFailedAttempts,
                       @Value("${app.auth.login.failed-window-seconds:900}") long loginFailedWindowSeconds,
                       @Value("${app.auth.login.lock-seconds:900}") long loginLockSeconds) {
        this.authenticationManager = authenticationManager;
        this.usuarioRepository = usuarioRepository;
        this.pacienteRepository = pacienteRepository;
        this.profesionalRepository = profesionalRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.invitationTokenRepository = invitationTokenRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtService = jwtService;
        this.accessControlService = accessControlService;
        this.auditService = auditService;
        this.emailService = emailService;
        this.userAccountEmailService = userAccountEmailService;
        this.passwordEncoder = passwordEncoder;
        this.refreshExpirationSeconds = refreshExpirationSeconds;
        this.passwordResetExpirationSeconds = passwordResetExpirationSeconds;
        this.passwordResetFrontendUrl = passwordResetFrontendUrl;
        this.exposePasswordResetToken = exposePasswordResetToken;
        this.loginMaxFailedAttempts = Math.max(loginMaxFailedAttempts, 1);
        this.loginFailedWindowSeconds = Math.max(loginFailedWindowSeconds, 1);
        this.loginLockSeconds = Math.max(loginLockSeconds, 1);
    }

    @Transactional
    public LoginResponse loginAsProfessional(LoginRequest request) {
        return login(request, UserRole.PROFESSIONAL);
    }

    @Transactional
    public LoginResponse loginAsPatient(LoginRequest request) {
        return login(request, UserRole.PATIENT);
    }

    @Transactional
    public LoginResponse login(LoginRequest request, UserRole requestedRole) {
        String normalizedEmail = normalizeLoginKey(request.email());
        assertLoginNotLocked(normalizedEmail);

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(normalizedEmail, request.password())
            );
        } catch (AuthenticationException ex) {
            registerFailedLogin(normalizedEmail);
            throw new ConflictException("Credenciales inválidas");
        }

        clearFailedLogins(normalizedEmail);

        Usuario usuario = usuarioRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        if (!Boolean.TRUE.equals(usuario.getActive())) {
            throw new ConflictException("Usuario inactivo");
        }

        usuario.setUltimoLogin(OffsetDateTime.now());

        UserRole sessionRole = resolveSessionRole(usuario, requestedRole);

        String accessToken = jwtService.generateAccessToken(usuario, sessionRole);
        String refreshToken = createAndPersistRefreshToken(usuario, sessionRole);
        auditService.register("LOGIN", "usuarios", usuario.getId(), "role=" + sessionRole.name());

        return new LoginResponse(
                accessToken,
                refreshToken,
                "Bearer",
                jwtService.getAccessTokenExpirationSeconds(),
                usuario.getId(),
                usuario.getEmail(),
                usuario.getDisplayName(),
            sessionRole
        );
    }

    @Transactional
    public LoginResponse refresh(String plainRefreshToken) {
        String tokenHash = hashToken(plainRefreshToken);

        RefreshToken stored = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new ResourceNotFoundException("Refresh token inválido"));

        if (stored.isRevoked() || stored.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new ConflictException("Refresh token expirado o revocado");
        }

        stored.setRevoked(true);

        Usuario usuario = stored.getUser();
        UserRole sessionRole = stored.getSelectedRole() != null ? stored.getSelectedRole() : usuario.getRole();
        if (sessionRole == null) {
            throw new ConflictException("No se pudo determinar el rol de sesión");
        }
        String newAccessToken = jwtService.generateAccessToken(usuario, sessionRole);
        String newRefreshToken = createAndPersistRefreshToken(usuario, sessionRole);

        return new LoginResponse(
                newAccessToken,
                newRefreshToken,
                "Bearer",
                jwtService.getAccessTokenExpirationSeconds(),
                usuario.getId(),
                usuario.getEmail(),
                usuario.getDisplayName(),
            sessionRole
        );
    }

    @Transactional
    public LoginResponse switchRole(String plainRefreshToken, UserRole requestedRole) {
        String tokenHash = hashToken(plainRefreshToken);

        RefreshToken stored = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new ResourceNotFoundException("Refresh token inválido"));

        if (stored.isRevoked() || stored.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new ConflictException("Refresh token expirado o revocado");
        }

        Usuario usuario = stored.getUser();
        UserRole sessionRole = resolveSessionRole(usuario, requestedRole);

        stored.setRevoked(true);

        String newAccessToken = jwtService.generateAccessToken(usuario, sessionRole);
        String newRefreshToken = createAndPersistRefreshToken(usuario, sessionRole);
        auditService.register("SWITCH_ROLE", "usuarios", usuario.getId(), "role=" + sessionRole.name());

        return new LoginResponse(
                newAccessToken,
                newRefreshToken,
                "Bearer",
                jwtService.getAccessTokenExpirationSeconds(),
                usuario.getId(),
                usuario.getEmail(),
                usuario.getDisplayName(),
                sessionRole
        );
    }

    @Transactional
    public void logout(String plainRefreshToken) {
        String tokenHash = hashToken(plainRefreshToken);
        refreshTokenRepository.findByTokenHash(tokenHash).ifPresent(token -> {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
            auditService.register("LOGOUT", "usuarios", token.getUser().getId(), null);
        });
    }

    @Transactional(readOnly = true)
    public CheckEmailResponse checkEmail(String email) {
        String normalizedEmail = normalizeLoginKey(email);
        Usuario usuario = usuarioRepository.findByEmail(normalizedEmail).orElse(null);
        
        if (usuario == null) {
            return new CheckEmailResponse(false, false, false);
        }
        
        boolean hasPatientProfile = pacienteRepository.existsById(usuario.getId());
        boolean hasProfessionalProfile = profesionalRepository.existsById(usuario.getId());
        
        return new CheckEmailResponse(true, hasPatientProfile, hasProfessionalProfile);
    }

    @Transactional(readOnly = true)
    public MeResponse me() {
        Usuario actor = accessControlService.currentUsuario();
        return new MeResponse(
                actor.getId(),
                actor.getEmail(),
                actor.getDisplayName(),
                accessControlService.currentUserRole(),
                actor.getActive()
        );
    }

    @Transactional
    public PasswordResetRequestResponse requestPasswordReset(PasswordResetRequest request) {
        String genericMessage = "Si el correo existe, se ha generado un token de recuperación";
        String normalizedEmail = normalizeLoginKey(request.email());

        return usuarioRepository.findByEmail(normalizedEmail)
                .map(user -> {
                    if (!Boolean.TRUE.equals(user.getActive())) {
                        return new PasswordResetRequestResponse(genericMessage, null);
                    }

                    String plainToken = UUID.randomUUID() + "." + UUID.randomUUID();

                    passwordResetTokenRepository.deleteByUser_Id(user.getId());

                    PasswordResetToken token = new PasswordResetToken();
                    token.setUser(user);
                    token.setTokenHash(hashToken(plainToken));
                    token.setExpiresAt(OffsetDateTime.now().plusSeconds(passwordResetExpirationSeconds));
                    token.setUsed(false);
                    passwordResetTokenRepository.save(token);
                    auditService.register("PASSWORD_RESET_REQUEST", "usuarios", user.getId());
                    sendPasswordResetEmail(user, plainToken, token.getExpiresAt());

                    return new PasswordResetRequestResponse(
                            genericMessage,
                            exposePasswordResetToken ? plainToken : null
                    );
                })
                .orElseGet(() -> new PasswordResetRequestResponse(genericMessage, null));
    }

    @Transactional
    public void confirmPasswordReset(PasswordResetConfirmRequest request) {
        String hashedToken = hashToken(request.token());

        PasswordResetToken resetToken = passwordResetTokenRepository.findByTokenHash(hashedToken)
                .orElseThrow(() -> new ConflictException("Token inválido o expirado"));

        if (resetToken.isUsed() || resetToken.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new ConflictException("Token inválido o expirado");
        }

        Usuario user = resetToken.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));

        resetToken.setUsed(true);
        resetToken.setUsedAt(OffsetDateTime.now());

        refreshTokenRepository.findAllByUser_IdAndRevokedFalse(user.getId())
                .forEach(token -> token.setRevoked(true));
        auditService.register("PASSWORD_RESET_CONFIRM", "usuarios", user.getId());
    }

    @Transactional
    public void acceptProfessionalInvitation(AcceptProfessionalInvitationRequest request) {
        String tokenHash = hashToken(request.token());

        ProfessionalInvitationToken invitation = invitationTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new ConflictException("Token inválido o expirado"));

        if (invitation.isUsed() || invitation.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new ConflictException("Token inválido o expirado");
        }

        Usuario user = invitation.getUser();
        if (user.getRole() != UserRole.PROFESSIONAL && user.getRole() != UserRole.PATIENT) {
            throw new ConflictException("La invitación no corresponde a una cuenta activable");
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setActive(true);

        invitation.setUsed(true);
        invitation.setUsedAt(OffsetDateTime.now());
        auditService.register("INVITATION_ACCEPT", "usuarios", user.getId());
    }

    @Transactional
    public RegisterPatientResponse registerPatient(RegisterPatientRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();
        Usuario existingUser = usuarioRepository.findByEmail(normalizedEmail).orElse(null);
        if (existingUser != null) {
            if (existingUser.getRole() != UserRole.PROFESSIONAL && existingUser.getRole() != UserRole.PATIENT) {
                throw new ConflictException("Ya existe un usuario con el email indicado");
            }
            if (pacienteRepository.existsById(existingUser.getId())) {
                throw new ConflictException("Ya existe un paciente con el email indicado");
            }
        }

        String normalizedRut = RutUtils.normalize(request.rut());
        if (pacienteRepository.existsByRut(normalizedRut)) {
            throw new ConflictException("Ya existe un paciente con el RUT indicado");
        }

        Usuario user;
        if (existingUser != null) {
            user = existingUser;
            if (request.displayName() != null && !request.displayName().isBlank()) {
                user.setDisplayName(request.displayName());
            }
            if (!Boolean.TRUE.equals(user.getActive())) {
                user.setActive(true);
            }
            usuarioRepository.save(user);
        } else {
            if (request.password() == null || request.password().isBlank()) {
                throw new ConflictException("Se requiere contraseña para crear una cuenta nueva");
            }
            user = new Usuario();
            user.setEmail(normalizedEmail);
            user.setDisplayName(request.displayName());
            user.setRole(UserRole.PATIENT);
            user.setActive(true);
            user.setPasswordHash(passwordEncoder.encode(request.password()));
            usuarioRepository.save(user);
            userAccountEmailService.sendUserCreatedEmail(user);
        }

        Paciente paciente = new Paciente();
        paciente.setUsuario(user);
        paciente.setProfesional(null);
        paciente.setRut(normalizedRut);
        pacienteRepository.save(paciente);
        auditService.register("REGISTER_PATIENT", "usuarios", user.getId());

        return new RegisterPatientResponse(
                user.getId(),
                user.getEmail(),
                user.getRole(),
                user.getActive()
        );
    }

    private void assertLoginNotLocked(String loginKey) {
        FailedLoginAttempt state = failedLogins.get(loginKey);
        if (state == null) {
            return;
        }

        OffsetDateTime now = OffsetDateTime.now();
        if (state.lockedUntil != null && state.lockedUntil.isAfter(now)) {
            throw new ConflictException("Cuenta temporalmente bloqueada por intentos fallidos. Intenta más tarde");
        }

        if (state.lockedUntil != null && !state.lockedUntil.isAfter(now)) {
            failedLogins.remove(loginKey);
        }
    }

    private void registerFailedLogin(String loginKey) {
        OffsetDateTime now = OffsetDateTime.now();
        failedLogins.compute(loginKey, (key, current) -> {
            FailedLoginAttempt state = current;
            if (state == null || state.firstFailureAt.plusSeconds(loginFailedWindowSeconds).isBefore(now)) {
                state = new FailedLoginAttempt(now, 0, null);
            }

            int failedCount = state.failedCount + 1;
            OffsetDateTime lockedUntil = state.lockedUntil;

            if (failedCount >= loginMaxFailedAttempts) {
                lockedUntil = now.plusSeconds(loginLockSeconds);
            }

            return new FailedLoginAttempt(state.firstFailureAt, failedCount, lockedUntil);
        });
    }

    private void clearFailedLogins(String loginKey) {
        failedLogins.remove(loginKey);
    }

    private String normalizeLoginKey(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    private String createAndPersistRefreshToken(Usuario usuario, UserRole selectedRole) {
        String plainToken = UUID.randomUUID() + "." + UUID.randomUUID();

        RefreshToken token = new RefreshToken();
        token.setUser(usuario);
        token.setSelectedRole(selectedRole);
        token.setTokenHash(hashToken(plainToken));
        token.setExpiresAt(OffsetDateTime.now().plusSeconds(refreshExpirationSeconds));
        token.setRevoked(false);
        refreshTokenRepository.save(token);

        return plainToken;
    }

    private UserRole resolveSessionRole(Usuario usuario, UserRole requestedRole) {
        if (requestedRole == null) {
            return usuario.getRole();
        }

        if (usuario.getRole() == UserRole.ADMIN) {
            return UserRole.ADMIN;
        }

        return switch (requestedRole) {
            case PATIENT -> {
                if (!pacienteRepository.existsById(usuario.getId())) {
                    throw new ConflictException("La cuenta no tiene perfil de paciente");
                }
                yield UserRole.PATIENT;
            }
            case PROFESSIONAL -> {
                if (!profesionalRepository.existsById(usuario.getId())) {
                    throw new ConflictException("La cuenta no tiene perfil profesional");
                }
                yield UserRole.PROFESSIONAL;
            }
            default -> usuario.getRole();
        };
    }

        private String hashToken(String plainToken) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(plainToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo procesar refresh token", e);
        }
    }

    private void sendPasswordResetEmail(Usuario user, String plainToken, OffsetDateTime expiresAt) {
        String subject = "Recuperación de contraseña - GESPA";
        String resetLink = buildPasswordResetLink(plainToken);
        String htmlBody = buildPasswordResetHtml(user, plainToken, resetLink, expiresAt);
        emailService.sendHtml(user.getEmail(), subject, htmlBody);
    }

    private String buildPasswordResetLink(String plainToken) {
        String encodedToken = URLEncoder.encode(plainToken, StandardCharsets.UTF_8);
        String frontendUrl = normalizePasswordResetFrontendUrl(passwordResetFrontendUrl);

        if (frontendUrl.contains("{token}")) {
            return frontendUrl.replace("{token}", encodedToken);
        }

        String separator = frontendUrl.contains("?") ? "&" : "?";
        return frontendUrl + separator + "token=" + encodedToken;
    }

    private String normalizePasswordResetFrontendUrl(String configuredUrl) {
        String value = configuredUrl == null ? "" : configuredUrl.trim();
        if (value.isBlank()) {
            return "http://localhost:5173/reset-password";
        }

        if (value.contains("{token}") || value.toLowerCase().contains("reset-password")) {
            return value;
        }

        if (value.endsWith("/")) {
            return value + "reset-password";
        }

        return value + "/reset-password";
    }

    private String buildPasswordResetHtml(Usuario user, String plainToken, String resetLink, OffsetDateTime expiresAt) {
        String displayName = user.getDisplayName() == null || user.getDisplayName().isBlank()
                ? "usuario"
                : user.getDisplayName();
        long expiresInMinutes = Math.max(1, OffsetDateTime.now().until(expiresAt, ChronoUnit.MINUTES));

        String safeDisplayName = escapeHtml(displayName);
        String safeToken = escapeHtml(plainToken);
        String safeResetLink = escapeHtml(resetLink);

        return """
                <html>
                    <body style=\"font-family: Arial, sans-serif; color: #1f2937; background: #f8fafc; padding: 24px;\">
                        <table style=\"max-width: 620px; margin: 0 auto; background: #ffffff; border: 1px solid #e5e7eb; border-radius: 10px; padding: 24px;\">
                            <tr>
                                <td>
                                    <h2 style=\"margin: 0 0 12px 0; color: #111827;\">Recuperación de contraseña</h2>
                                    <p>Hola %s,</p>
                                    <p>Recibimos una solicitud para restablecer tu contraseña en GESPA.</p>

                                    <p style=\"margin: 16px 0 8px 0;\"><strong>Tu token de recuperación:</strong></p>
                                    <div style=\"margin: 0 0 18px 0; padding: 12px 14px; border: 1px dashed #94a3b8; border-radius: 8px; background: #f8fafc; font-family: 'Courier New', monospace; font-size: 20px; letter-spacing: 1.5px; font-weight: 700; color: #0f172a; text-align: center;\">%s</div>

                                    <p style=\"margin: 0 0 4px 0;\"><strong>Pasos:</strong></p>
                                    <ol style=\"margin-top: 6px; padding-left: 18px;\">
                                        <li>Abre la página de recuperación de contraseña.</li>
                                        <li>Pega el token en el campo correspondiente.</li>
                                        <li>Ingresa tu nueva contraseña y confirma.</li>
                                    </ol>

                                    <p style=\"margin: 24px 0;\">
                                        <a href=\"%s\" style=\"background: #2563eb; color: #ffffff; text-decoration: none; padding: 12px 18px; border-radius: 8px; display: inline-block;\">
                                            Ir a recuperar contraseña
                                        </a>
                                    </p>

                                    <p>Este token vence en aproximadamente <strong>%d minutos</strong>.</p>
                                    <p>Si no solicitaste este cambio, puedes ignorar este correo.</p>
                                    <hr style=\"border: none; border-top: 1px solid #e5e7eb; margin: 24px 0;\"/>
                                    <p style=\"font-size: 12px; color: #6b7280;\">
                                        Si el botón no funciona, copia y pega este enlace en tu navegador:<br/>
                                        <a href=\"%s\">%s</a>
                                    </p>
                                </td>
                            </tr>
                        </table>
                    </body>
                </html>
                """.formatted(safeDisplayName, safeToken, safeResetLink, expiresInMinutes, safeResetLink, safeResetLink);
    }

        private String escapeHtml(String value) {
                return value
                                .replace("&", "&amp;")
                                .replace("<", "&lt;")
                                .replace(">", "&gt;")
                                .replace("\"", "&quot;")
                                .replace("'", "&#39;");
        }

    private record FailedLoginAttempt(OffsetDateTime firstFailureAt, int failedCount, OffsetDateTime lockedUntil) {
    }
}
