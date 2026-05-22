package com.tbtha.gespa_backend.dtos;

import jakarta.validation.constraints.NotBlank;

public record AdminCreateSpecialtyRequest(
        @NotBlank String name
) {
}
