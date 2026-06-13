package com.tbtha.gespa_backend.services.email;

import com.tbtha.gespa_backend.entities.Usuario;
import com.tbtha.gespa_backend.entities.enums.UserRole;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

@Service
public class UserAccountEmailService {

    private final EmailService emailService;
    private final String invitationFrontendUrl;

    public UserAccountEmailService(
            EmailService emailService,
            @Value("${app.auth.invitation.frontend-url:http://localhost:5173/aceptar-invitacion}") String invitationFrontendUrl
    ) {
        this.emailService = emailService;
        this.invitationFrontendUrl = invitationFrontendUrl;
    }

    public void sendUserCreatedEmail(Usuario user) {
        if (user == null || user.getEmail() == null || user.getEmail().isBlank()) {
            return;
        }

        String subject = "¡Bienvenido a GESPA! Tu cuenta ha sido creada";
        String htmlBody = buildUserCreatedHtml(user);
        emailService.sendHtml(user.getEmail(), subject, htmlBody);
    }

    public void sendActivationInvitationEmail(Usuario user, String plainToken, OffsetDateTime expiresAt) {
        if (user == null || user.getEmail() == null || user.getEmail().isBlank()) {
            return;
        }
        if (plainToken == null || plainToken.isBlank() || expiresAt == null) {
            return;
        }

        String subject = "Activa tu cuenta en GESPA";
        String activationLink = buildInvitationActivationLink(plainToken);
        String htmlBody = buildActivationInvitationHtml(user, plainToken, activationLink, expiresAt);
        emailService.sendHtml(user.getEmail(), subject, htmlBody);
    }

    private String buildUserCreatedHtml(Usuario user) {
        String displayName = user.getDisplayName() == null || user.getDisplayName().isBlank()
                ? "usuario"
                : user.getDisplayName();

        String safeDisplayName = escapeHtml(displayName);
        String safeEmail = escapeHtml(user.getEmail());
        String safeRole = escapeHtml(toRoleLabel(user.getRole()));

        String activationMessage = Boolean.TRUE.equals(user.getActive())
                ? "Tu cuenta ya está activa. Puedes iniciar sesión con tu correo y contraseña."
                : "Tu cuenta está creada pero pendiente de activación. Sigue las instrucciones entregadas para completar tu registro.";

        return """
                <html>
                    <body style=\"font-family: Arial, sans-serif; color: #1f2937; background: #f8fafc; padding: 24px;\">
                        <table style=\"max-width: 620px; margin: 0 auto; background: #ffffff; border: 1px solid #e5e7eb; border-radius: 10px; padding: 24px;\">
                            <tr>
                                <td>
                                    <h2 style=\"margin: 0 0 12px 0; color: #111827;\">¡Bienvenido a GESPA!</h2>
                                    <p>Hola %s,</p>
                                    <p>Tu cuenta ha sido creada exitosamente en nuestra plataforma.</p>

                                    <p style=\"margin: 16px 0 8px 0;\"><strong>Datos de tu cuenta:</strong></p>
                                    <div style=\"margin: 0 0 18px 0; padding: 12px 14px; border: 1px solid #e5e7eb; border-radius: 8px; background: #f8fafc; font-size: 14px;\">
                                        <p style=\"margin: 4px 0;\"><strong>Correo:</strong> %s</p>
                                        <p style=\"margin: 4px 0;\"><strong>Tipo de cuenta:</strong> %s</p>
                                    </div>

                                    <p>%s</p>

                                    <p style=\"margin: 24px 0;\">
                                        <a href=\"http://localhost:5173/login\" style=\"background: #2563eb; color: #ffffff; text-decoration: none; padding: 12px 18px; border-radius: 8px; display: inline-block;\">
                                            Ir a iniciar sesión
                                        </a>
                                    </p>

                                    <hr style=\"border: none; border-top: 1px solid #e5e7eb; margin: 24px 0;\"/>
                                    <p style=\"font-size: 12px; color: #6b7280;\">Si no reconoces esta creación de cuenta o tienes dudas, contacta al equipo de soporte de GESPA.</p>
                                </td>
                            </tr>
                        </table>
                    </body>
                </html>
                """.formatted(safeDisplayName, safeEmail, safeRole, escapeHtml(activationMessage));
    }

    private String toRoleLabel(UserRole role) {
        if (role == null) {
            return "Usuario";
        }

        return switch (role) {
            case ADMIN -> "Administrador";
            case PROFESSIONAL -> "Profesional";
            case PATIENT -> "Paciente";
        };
    }

    private String buildInvitationActivationLink(String plainToken) {
        String encodedToken = URLEncoder.encode(plainToken, StandardCharsets.UTF_8);
        String frontendUrl = normalizeInvitationFrontendUrl(invitationFrontendUrl);

        if (frontendUrl.contains("{token}")) {
            return frontendUrl.replace("{token}", encodedToken);
        }

        String separator = frontendUrl.contains("?") ? "&" : "?";
        return frontendUrl + separator + "token=" + encodedToken;
    }

    private String normalizeInvitationFrontendUrl(String configuredUrl) {
        String value = configuredUrl == null ? "" : configuredUrl.trim();
        if (value.isBlank()) {
            return "http://localhost:5173/aceptar-invitacion";
        }

        if (value.contains("{token}") || value.toLowerCase().contains("aceptar-invitacion")) {
            return value;
        }

        if (value.endsWith("/")) {
            return value + "aceptar-invitacion";
        }

        return value + "/aceptar-invitacion";
    }

    private String buildActivationInvitationHtml(Usuario user, String plainToken, String activationLink, OffsetDateTime expiresAt) {
        String displayName = user.getDisplayName() == null || user.getDisplayName().isBlank()
                ? "usuario"
                : user.getDisplayName();
        long expiresInMinutes = Math.max(1, OffsetDateTime.now().until(expiresAt, ChronoUnit.MINUTES));

        String safeDisplayName = escapeHtml(displayName);
        String safeToken = escapeHtml(plainToken);
        String safeActivationLink = escapeHtml(activationLink);
        String safeRole = escapeHtml(toRoleLabel(user.getRole()));

        return """
                <html>
                    <body style=\"font-family: Arial, sans-serif; color: #1f2937; background: #f8fafc; padding: 24px;\">
                        <table style=\"max-width: 620px; margin: 0 auto; background: #ffffff; border: 1px solid #e5e7eb; border-radius: 10px; padding: 24px;\">
                            <tr>
                                <td>
                                    <h2 style=\"margin: 0 0 12px 0; color: #111827;\">Activación de cuenta</h2>
                                    <p>Hola %s,</p>
                                    <p>Se creó una cuenta de tipo <strong>%s</strong> para ti en GESPA.</p>

                                    <p style=\"margin: 16px 0 8px 0;\"><strong>Tu token de activación:</strong></p>
                                    <div style=\"margin: 0 0 18px 0; padding: 12px 14px; border: 1px dashed #94a3b8; border-radius: 8px; background: #f8fafc; font-family: 'Courier New', monospace; font-size: 20px; letter-spacing: 1.5px; font-weight: 700; color: #0f172a; text-align: center;\">%s</div>

                                    <p style=\"margin: 0 0 4px 0;\"><strong>Pasos:</strong></p>
                                    <ol style=\"margin-top: 6px; padding-left: 18px;\">
                                        <li>Abre la página de activación.</li>
                                        <li>Confirma o pega el token de invitación.</li>
                                        <li>Define tu contraseña para activar tu cuenta.</li>
                                    </ol>

                                    <p style=\"margin: 24px 0;\">
                                        <a href=\"%s\" style=\"background: #2563eb; color: #ffffff; text-decoration: none; padding: 12px 18px; border-radius: 8px; display: inline-block;\">
                                            Ir a activar cuenta
                                        </a>
                                    </p>

                                    <p>Este token vence en aproximadamente <strong>%d minutos</strong>.</p>
                                    <p>Si no reconoces esta invitación, ignora este correo o contacta a soporte.</p>
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
                """.formatted(safeDisplayName, safeRole, safeToken, safeActivationLink, expiresInMinutes, safeActivationLink, safeActivationLink);
    }

    private String escapeHtml(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
