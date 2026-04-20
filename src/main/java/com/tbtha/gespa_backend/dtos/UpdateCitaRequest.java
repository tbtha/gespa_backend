package com.tbtha.gespa_backend.dtos;

import com.tbtha.gespa_backend.entities.enums.AppointmentStatus;
import com.tbtha.gespa_backend.entities.enums.ModalidadAtencion;
import com.tbtha.gespa_backend.entities.enums.TipoAtencion;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;

public record UpdateCitaRequest(
        @NotNull Long pacienteId,
        @NotNull Long profesionalId,
        @NotNull OffsetDateTime startsAt,
        @NotNull OffsetDateTime endsAt,
        TipoAtencion tipoAtencion,
        ModalidadAtencion modalidad,
        String reason,
        String location,
        AppointmentStatus status
) {
}
