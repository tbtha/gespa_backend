package com.tbtha.gespa_backend.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateNotaRequest(
        @NotNull Long professionalId,
        @NotNull Long appointmentId,
        @NotBlank String noteType,
        @NotBlank String content,
        String indicaciones,
        String plan,
        Boolean isPrivate
) {
}
