package com.tbtha.gespa_backend.services.email;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Service
@Primary
public class TransportAwareEmailService implements EmailService {

    private final SmtpEmailService smtpEmailService;
    private final BrevoHttpEmailService brevoHttpEmailService;
    private final String transport;

    public TransportAwareEmailService(SmtpEmailService smtpEmailService,
                                      BrevoHttpEmailService brevoHttpEmailService,
                                      @Value("${app.mail.transport:auto}") String transport) {
        this.smtpEmailService = smtpEmailService;
        this.brevoHttpEmailService = brevoHttpEmailService;
        this.transport = transport == null ? "auto" : transport.trim().toLowerCase();
    }

    @Override
    public void sendHtml(String to, String subject, String htmlBody) {
        switch (transport) {
            case "http" -> brevoHttpEmailService.sendHtml(to, subject, htmlBody);
            case "smtp" -> smtpEmailService.sendHtml(to, subject, htmlBody);
            case "auto" -> {
                if (brevoHttpEmailService.isEnabled()) {
                    brevoHttpEmailService.sendHtml(to, subject, htmlBody);
                } else {
                    smtpEmailService.sendHtml(to, subject, htmlBody);
                }
            }
            default -> {
                if (brevoHttpEmailService.isEnabled()) {
                    brevoHttpEmailService.sendHtml(to, subject, htmlBody);
                } else {
                    smtpEmailService.sendHtml(to, subject, htmlBody);
                }
            }
        }
    }
}
