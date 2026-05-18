package com.tbtha.gespa_backend.dtos;

import com.tbtha.gespa_backend.entities.enums.UserRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SwitchRoleRequest(
        @NotBlank String refreshToken,
        @NotNull UserRole role
) {
}
