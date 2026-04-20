package com.tbtha.gespa_backend.dtos.dashboard;

import com.tbtha.gespa_backend.entities.enums.AppointmentStatus;
import com.tbtha.gespa_backend.entities.enums.ModalidadAtencion;
import com.tbtha.gespa_backend.entities.enums.TipoAtencion;

import java.time.OffsetDateTime;

public record DashboardCitaItemResponse(
        Long citaId,
        Long pacienteId,
        String pacienteNombre,
        OffsetDateTime startsAt,
        OffsetDateTime endsAt,
        AppointmentStatus status,
        TipoAtencion tipoAtencion,
        ModalidadAtencion modalidad
) {
}
