package com.tbtha.gespa_backend.dtos;

import jakarta.validation.constraints.NotNull;

public record AdminUpdateUserStatusRequest(
        @NotNull Boolean active
) {
}
