package com.tbtha.gespa_backend.services.email;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

/**
 * Implementación principal de EmailService.
 * Delega siempre en BrevoHttpEmailService (API HTTP).
 * El transporte SMTP fue eliminado — se usa exclusivamente la API HTTP de Brevo.
 */
@Service
@Primary
public class TransportAwareEmailService implements EmailService {

    private final BrevoHttpEmailService brevoHttpEmailService;

    public TransportAwareEmailService(BrevoHttpEmailService brevoHttpEmailService) {
        this.brevoHttpEmailService = brevoHttpEmailService;
    }

    @Override
    public void sendHtml(String to, String subject, String htmlBody) {
        brevoHttpEmailService.sendHtml(to, subject, htmlBody);
    }
}
