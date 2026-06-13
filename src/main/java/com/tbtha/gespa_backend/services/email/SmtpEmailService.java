package com.tbtha.gespa_backend.services.email;

import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
public class SmtpEmailService implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(SmtpEmailService.class);

    private final JavaMailSender mailSender;
    private final String from;
    private final boolean enabled;

    public SmtpEmailService(JavaMailSender mailSender,
                            @Value("${app.mail.from:no-reply@gespa.cl}") String from,
                            @Value("${app.mail.enabled:true}") boolean enabled) {
        this.mailSender = mailSender;
        this.from = from;
        this.enabled = enabled;
    }

    @Override
    public void sendHtml(String to, String subject, String htmlBody) {
        if (!enabled) {
            log.debug("Envío de correos deshabilitado por configuración. Destinatario: {}", to);
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, StandardCharsets.UTF_8.name());
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
        } catch (Exception ex) {
            log.warn("No se pudo enviar correo a {}: {}", to, ex.getMessage());
            log.debug("Error detallado de envío de correo", ex);
        }
    }
}
