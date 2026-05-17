package com.tbtha.gespa_backend.dtos;

import com.tbtha.gespa_backend.entities.enums.UserRole;

public record RegisterPatientResponse(
        Long userId,
        String email,
        UserRole role,
        Boolean active
) {
}
