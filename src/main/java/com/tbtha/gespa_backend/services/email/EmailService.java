package com.tbtha.gespa_backend.services.email;

public interface EmailService {

    void sendHtml(String to, String subject, String htmlBody);
}
