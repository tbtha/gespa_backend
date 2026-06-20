package com.tbtha.gespa_backend.dtos;

public record AdminResetPasswordResponse(
        Long userId,
        String email,
        String temporaryPassword  // null — se envía correo de recuperación al usuario
) {
}
