package com.tbtha.gespa_backend.dtos;

import com.tbtha.gespa_backend.entities.enums.UserRole;

import java.time.OffsetDateTime;

public record PatientInvitationResponse(
        Long userId,
        Long patientId,
        String email,
        String displayName,
        String rut,
        Long professionalId,
        UserRole role,
        String inviteToken,
        OffsetDateTime expiresAt
) {
}
