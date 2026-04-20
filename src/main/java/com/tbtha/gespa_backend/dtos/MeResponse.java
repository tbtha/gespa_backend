package com.tbtha.gespa_backend.dtos;

import com.tbtha.gespa_backend.entities.enums.UserRole;

public record MeResponse(
        Long id,
        String email,
        String displayName,
        UserRole role,
        Boolean active
) {
}
