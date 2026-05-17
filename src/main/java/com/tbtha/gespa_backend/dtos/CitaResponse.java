package com.tbtha.gespa_backend.dtos;

import com.tbtha.gespa_backend.entities.enums.AppointmentStatus;
import com.tbtha.gespa_backend.entities.enums.ModalidadAtencion;
import com.tbtha.gespa_backend.entities.enums.TipoAtencion;

import java.time.OffsetDateTime;

public record CitaResponse(
        Long id,
        Long pacienteId,
        String pacienteNombre,
        Long profesionalId,
        String profesionalNombre,
        String profesionalEspecialidad,
        OffsetDateTime startsAt,
        OffsetDateTime endsAt,
        AppointmentStatus status,
        TipoAtencion tipoAtencion,
        ModalidadAtencion modalidad,
        String reason,
        String location
) {
}
