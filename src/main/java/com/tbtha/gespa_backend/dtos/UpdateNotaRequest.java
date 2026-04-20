package com.tbtha.gespa_backend.dtos;

import jakarta.validation.constraints.NotBlank;

public record UpdateNotaRequest(
        @NotBlank String noteType,
        @NotBlank String content,
        String indicaciones,
        String plan,
        Boolean isPrivate
) {
}
