package com.tbtha.gespa_backend.services.email;

import com.tbtha.gespa_backend.entities.Cita;
import com.tbtha.gespa_backend.entities.Paciente;
import com.tbtha.gespa_backend.entities.Profesional;
import com.tbtha.gespa_backend.entities.enums.ModalidadAtencion;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
public class AppointmentEmailService {

    private final EmailService emailService;
    private final String loginFrontendBaseUrl;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM 'de' yyyy", new Locale("es", "CL"));
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm", new Locale("es", "CL"));

    public AppointmentEmailService(
            EmailService emailService,
            @Value("${app.auth.login.frontend-url:http://localhost:5173}") String loginFrontendBaseUrl
    ) {
        this.emailService = emailService;
        this.loginFrontendBaseUrl = loginFrontendBaseUrl.replaceAll("/+$", "");
    }

    /**
     * Envía notificación de nueva cita al paciente
     */
    public void sendAppointmentConfirmationToPatient(Cita cita) {
        if (cita == null || cita.getPaciente() == null) {
            return;
        }

        Paciente paciente = cita.getPaciente();
        String email = paciente.getUsuario() != null ? paciente.getUsuario().getEmail() : null;

        if (email == null || email.isBlank()) {
            return;
        }

        String subject = "Confirmación de cita - GESPA";
        String htmlBody = buildPatientConfirmationHtml(cita);
        emailService.sendHtml(email, subject, htmlBody);
    }

    /**
     * Envía notificación de nueva cita al profesional
     */
    public void sendAppointmentNotificationToProfessional(Cita cita) {
        if (cita == null || cita.getProfesional() == null) {
            return;
        }

        Profesional profesional = cita.getProfesional();
        String email = profesional.getUsuario() != null ? profesional.getUsuario().getEmail() : null;

        if (email == null || email.isBlank()) {
            return;
        }

        String subject = "Nueva cita agendada - GESPA";
        String htmlBody = buildProfessionalNotificationHtml(cita);
        emailService.sendHtml(email, subject, htmlBody);
    }

    /**
     * Envía ambas notificaciones: al paciente y al profesional
     */
    public void sendAppointmentNotifications(Cita cita) {
        sendAppointmentConfirmationToPatient(cita);
        sendAppointmentNotificationToProfessional(cita);
    }

    private String buildPatientConfirmationHtml(Cita cita) {
        Paciente paciente = cita.getPaciente();
        Profesional profesional = cita.getProfesional();

        String patientName = getDisplayName(paciente.getUsuario().getDisplayName());
        String professionalName = getDisplayName(profesional.getUsuario().getDisplayName());
        String specialty = profesional.getSpecialty() != null ? profesional.getSpecialty() : "No especificada";

        String fecha = capitalizeFirst(cita.getStartsAt().format(DATE_FORMATTER));
        String horaInicio = cita.getStartsAt().format(TIME_FORMATTER);
        String horaFin = cita.getEndsAt().format(TIME_FORMATTER);

        String modalidad = formatModalidad(cita.getModalidad());
        String tipoAtencion = cita.getTipoAtencion() != null ? formatTipoAtencion(cita.getTipoAtencion().name()) : "Consulta";
        String lugar = cita.getLocation() != null && !cita.getLocation().isBlank() ? cita.getLocation() : "Por confirmar";
        String motivo = cita.getReason() != null && !cita.getReason().isBlank() ? cita.getReason() : "No especificado";

        String portalUrl = escapeHtml(loginFrontendBaseUrl + "/login-paciente");

        return """
                <html>
                    <body style="font-family: Arial, sans-serif; color: #1f2937; background: #f8fafc; padding: 24px;">
                        <table style="max-width: 620px; margin: 0 auto; background: #ffffff; border: 1px solid #e5e7eb; border-radius: 10px; padding: 24px;">
                            <tr>
                                <td>
                                    <div style="text-align: center; margin-bottom: 20px;">
                                        <div style="display: inline-block; background: #dcfce7; color: #166534; padding: 8px 16px; border-radius: 20px; font-weight: 600; font-size: 14px;">
                                            ✓ Cita Confirmada
                                        </div>
                                    </div>
                                    
                                    <h2 style="margin: 0 0 12px 0; color: #111827;">Confirmación de tu cita</h2>
                                    <p>Hola %s,</p>
                                    <p>Tu cita ha sido agendada exitosamente. A continuación los detalles:</p>

                                    <div style="margin: 20px 0; padding: 16px; border: 1px solid #e5e7eb; border-radius: 10px; background: #f8fafc;">
                                        <table style="width: 100%%; font-size: 14px;">
                                            <tr>
                                                <td style="padding: 8px 0; color: #6b7280; width: 140px;"><strong>Profesional:</strong></td>
                                                <td style="padding: 8px 0;">%s</td>
                                            </tr>
                                            <tr>
                                                <td style="padding: 8px 0; color: #6b7280;"><strong>Especialidad:</strong></td>
                                                <td style="padding: 8px 0;">%s</td>
                                            </tr>
                                            <tr>
                                                <td style="padding: 8px 0; color: #6b7280;"><strong>Fecha:</strong></td>
                                                <td style="padding: 8px 0;">%s</td>
                                            </tr>
                                            <tr>
                                                <td style="padding: 8px 0; color: #6b7280;"><strong>Horario:</strong></td>
                                                <td style="padding: 8px 0;">%s - %s hrs</td>
                                            </tr>
                                            <tr>
                                                <td style="padding: 8px 0; color: #6b7280;"><strong>Tipo de atención:</strong></td>
                                                <td style="padding: 8px 0;">%s</td>
                                            </tr>
                                            <tr>
                                                <td style="padding: 8px 0; color: #6b7280;"><strong>Modalidad:</strong></td>
                                                <td style="padding: 8px 0;">%s</td>
                                            </tr>
                                            <tr>
                                                <td style="padding: 8px 0; color: #6b7280;"><strong>Lugar:</strong></td>
                                                <td style="padding: 8px 0;">%s</td>
                                            </tr>
                                            <tr>
                                                <td style="padding: 8px 0; color: #6b7280;"><strong>Motivo:</strong></td>
                                                <td style="padding: 8px 0;">%s</td>
                                            </tr>
                                        </table>
                                    </div>

                                    <p style="margin: 24px 0;">
                                        <a href="%s" style="background: #2563eb; color: #ffffff; text-decoration: none; padding: 12px 18px; border-radius: 8px; display: inline-block;">
                                            Ver mis citas en GESPA
                                        </a>
                                    </p>

                                    <hr style="border: none; border-top: 1px solid #e5e7eb; margin: 24px 0;"/>
                                    <p style="font-size: 12px; color: #6b7280;">
                                        Si necesitas cancelar o modificar tu cita, por favor comunícate con tu profesional de salud o ingresa a tu portal de paciente.
                                    </p>
                                </td>
                            </tr>
                        </table>
                    </body>
                </html>
                """.formatted(
                escapeHtml(patientName),
                escapeHtml(professionalName),
                escapeHtml(specialty),
                escapeHtml(fecha),
                escapeHtml(horaInicio),
                escapeHtml(horaFin),
                escapeHtml(tipoAtencion),
                escapeHtml(modalidad),
                escapeHtml(lugar),
                escapeHtml(motivo),
                portalUrl
        );
    }

    private String buildProfessionalNotificationHtml(Cita cita) {
        Paciente paciente = cita.getPaciente();
        Profesional profesional = cita.getProfesional();

        String professionalName = getDisplayName(profesional.getUsuario().getDisplayName());
        String patientName = getDisplayName(paciente.getUsuario().getDisplayName());
        String patientRut = paciente.getRut() != null ? paciente.getRut() : "No registrado";

        String fecha = capitalizeFirst(cita.getStartsAt().format(DATE_FORMATTER));
        String horaInicio = cita.getStartsAt().format(TIME_FORMATTER);
        String horaFin = cita.getEndsAt().format(TIME_FORMATTER);

        String modalidad = formatModalidad(cita.getModalidad());
        String tipoAtencion = cita.getTipoAtencion() != null ? formatTipoAtencion(cita.getTipoAtencion().name()) : "Consulta";
        String lugar = cita.getLocation() != null && !cita.getLocation().isBlank() ? cita.getLocation() : "Por confirmar";
        String motivo = cita.getReason() != null && !cita.getReason().isBlank() ? cita.getReason() : "No especificado";

        String agendaUrl = escapeHtml(loginFrontendBaseUrl + "/login-profesional");

        return """
                <html>
                    <body style="font-family: Arial, sans-serif; color: #1f2937; background: #f8fafc; padding: 24px;">
                        <table style="max-width: 620px; margin: 0 auto; background: #ffffff; border: 1px solid #e5e7eb; border-radius: 10px; padding: 24px;">
                            <tr>
                                <td>
                                    <div style="text-align: center; margin-bottom: 20px;">
                                        <div style="display: inline-block; background: #dbeafe; color: #1e40af; padding: 8px 16px; border-radius: 20px; font-weight: 600; font-size: 14px;">
                                            📅 Nueva Cita Agendada
                                        </div>
                                    </div>
                                    
                                    <h2 style="margin: 0 0 12px 0; color: #111827;">Tienes una nueva cita</h2>
                                    <p>Hola %s,</p>
                                    <p>Se ha agendado una nueva cita en tu agenda. A continuación los detalles:</p>

                                    <div style="margin: 20px 0; padding: 16px; border: 1px solid #e5e7eb; border-radius: 10px; background: #f8fafc;">
                                        <table style="width: 100%%; font-size: 14px;">
                                            <tr>
                                                <td style="padding: 8px 0; color: #6b7280; width: 140px;"><strong>Paciente:</strong></td>
                                                <td style="padding: 8px 0;">%s</td>
                                            </tr>
                                            <tr>
                                                <td style="padding: 8px 0; color: #6b7280;"><strong>RUT:</strong></td>
                                                <td style="padding: 8px 0;">%s</td>
                                            </tr>
                                            <tr>
                                                <td style="padding: 8px 0; color: #6b7280;"><strong>Fecha:</strong></td>
                                                <td style="padding: 8px 0;">%s</td>
                                            </tr>
                                            <tr>
                                                <td style="padding: 8px 0; color: #6b7280;"><strong>Horario:</strong></td>
                                                <td style="padding: 8px 0;">%s - %s hrs</td>
                                            </tr>
                                            <tr>
                                                <td style="padding: 8px 0; color: #6b7280;"><strong>Tipo de atención:</strong></td>
                                                <td style="padding: 8px 0;">%s</td>
                                            </tr>
                                            <tr>
                                                <td style="padding: 8px 0; color: #6b7280;"><strong>Modalidad:</strong></td>
                                                <td style="padding: 8px 0;">%s</td>
                                            </tr>
                                            <tr>
                                                <td style="padding: 8px 0; color: #6b7280;"><strong>Lugar:</strong></td>
                                                <td style="padding: 8px 0;">%s</td>
                                            </tr>
                                            <tr>
                                                <td style="padding: 8px 0; color: #6b7280;"><strong>Motivo:</strong></td>
                                                <td style="padding: 8px 0;">%s</td>
                                            </tr>
                                        </table>
                                    </div>

                                    <p style="margin: 24px 0;">
                                        <a href="%s" style="background: #2563eb; color: #ffffff; text-decoration: none; padding: 12px 18px; border-radius: 8px; display: inline-block;">
                                            Ver mi agenda en GESPA
                                        </a>
                                    </p>

                                    <hr style="border: none; border-top: 1px solid #e5e7eb; margin: 24px 0;"/>
                                    <p style="font-size: 12px; color: #6b7280;">
                                        Este correo es una notificación automática. Puedes gestionar tus citas desde tu panel de profesional.
                                    </p>
                                </td>
                            </tr>
                        </table>
                    </body>
                </html>
                """.formatted(
                escapeHtml(professionalName),
                escapeHtml(patientName),
                escapeHtml(patientRut),
                escapeHtml(fecha),
                escapeHtml(horaInicio),
                escapeHtml(horaFin),
                escapeHtml(tipoAtencion),
                escapeHtml(modalidad),
                escapeHtml(lugar),
                escapeHtml(motivo),
                agendaUrl
        );
    }

    private String getDisplayName(String name) {
        return (name == null || name.isBlank()) ? "Usuario" : name;
    }

    private String formatModalidad(ModalidadAtencion modalidad) {
        if (modalidad == null) return "Presencial";
        return switch (modalidad) {
            case PRESENCIAL -> "Presencial";
            case ONLINE -> "Online";
        };
    }

    private String formatTipoAtencion(String tipo) {
        if (tipo == null) return "Consulta";
        return switch (tipo.toUpperCase()) {
            case "PRIMERA_CONSULTA" -> "Primera consulta";
            case "CONTROL" -> "Control";
            case "URGENCIA" -> "Urgencia";
            case "PROCEDIMIENTO" -> "Procedimiento";
            case "EVALUACION" -> "Evaluación";
            case "SEGUIMIENTO" -> "Seguimiento";
            default -> tipo.replace("_", " ");
        };
    }

    private String capitalizeFirst(String text) {
        if (text == null || text.isEmpty()) return text;
        return text.substring(0, 1).toUpperCase() + text.substring(1);
    }

    private String escapeHtml(String value) {
        if (value == null) return "";
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
