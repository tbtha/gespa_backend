package com.tbtha.gespa_backend.dtos;

import com.tbtha.gespa_backend.entities.enums.UserRole;

import java.time.OffsetDateTime;

public record ProfessionalInvitationResponse(
        Long userId,
        String email,
        UserRole role,
        String inviteToken,
        OffsetDateTime expiresAt
) {
}
