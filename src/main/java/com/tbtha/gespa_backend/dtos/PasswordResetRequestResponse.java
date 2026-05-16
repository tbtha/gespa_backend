package com.tbtha.gespa_backend.dtos;

public record PasswordResetRequestResponse(
        String message,
        String resetToken
) {
}
