package com.tbtha.gespa_backend.services.email;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
public class BrevoHttpEmailService {

    private static final Logger log = LoggerFactory.getLogger(BrevoHttpEmailService.class);

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final String from;
    private final String fromName;
    private final String apiUrl;
    private final String apiKey;
    private final boolean enabled;
    private final long requestTimeoutMs;

    public BrevoHttpEmailService(ObjectMapper objectMapper,
                                 @Value("${app.mail.from:no-reply@gespa.cl}") String from,
                                 @Value("${app.mail.from-name:GESPA}") String fromName,
                                 @Value("${app.mail.http.api-url:https://api.brevo.com/v3/smtp/email}") String apiUrl,
                                 @Value("${app.mail.http.api-key:}") String apiKey,
                                 @Value("${app.mail.http.enabled:false}") boolean enabled,
                                 @Value("${app.mail.http.connect-timeout-ms:5000}") long connectTimeoutMs,
                                 @Value("${app.mail.http.request-timeout-ms:10000}") long requestTimeoutMs) {
        this.objectMapper = objectMapper;
        this.from = from;
        this.fromName = fromName == null || fromName.isBlank() ? "GESPA" : fromName.trim();
        this.apiUrl = apiUrl;
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.enabled = enabled;
        this.requestTimeoutMs = requestTimeoutMs;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(Math.max(connectTimeoutMs, 1000)))
                .build();
    }

    public void sendHtml(String to, String subject, String htmlBody) {
        if (!enabled) {
            log.debug("Envío HTTP de correos deshabilitado por configuración. Destinatario: {}", to);
            return;
        }

        if (apiKey.isBlank()) {
            log.warn("No se pudo enviar correo por API HTTP: falta app.mail.http.api-key");
            return;
        }

        try {
            Map<String, Object> payload = Map.of(
                    "sender", Map.of(
                            "email", from,
                            "name", fromName
                    ),
                    "to", List.of(Map.of("email", to)),
                    "subject", subject,
                    "htmlContent", htmlBody
            );

            String body = objectMapper.writeValueAsString(payload);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .timeout(Duration.ofMillis(Math.max(requestTimeoutMs, 1000)))
                    .header("accept", "application/json")
                    .header("content-type", "application/json")
                    .header("api-key", apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();

            if (status < 200 || status >= 300) {
                log.warn("No se pudo enviar correo por API HTTP a {}: status={}, body={}", to, status, response.body());
            }
        } catch (Exception ex) {
            log.warn("No se pudo enviar correo por API HTTP a {}: {}", to, ex.getMessage());
            log.debug("Error detallado de envío de correo por API HTTP", ex);
        }
    }

    public boolean isEnabled() {
        return enabled;
    }
}
