package com.tbtha.gespa_backend.dtos;

import java.time.OffsetDateTime;

public record NotaResponse(
        Long id,
        Long pacienteId,
        Long professionalId,
        Long appointmentId,
        String noteType,
        String content,
        String indicaciones,
        String plan,
        Boolean isPrivate,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
