package com.tbtha.gespa_backend.dtos;

import com.tbtha.gespa_backend.entities.enums.AppointmentStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateEstadoCitaRequest(
        @NotNull AppointmentStatus status
) {
}
