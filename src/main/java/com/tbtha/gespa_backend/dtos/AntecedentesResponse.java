package com.tbtha.gespa_backend.dtos;

import com.tbtha.gespa_backend.entities.enums.ActividadFisica;
import com.tbtha.gespa_backend.entities.enums.ConsumoNivel;
import com.tbtha.gespa_backend.entities.enums.ConsumoTabaco;

import java.time.OffsetDateTime;

public record AntecedentesResponse(
        Long id,
        Long pacienteId,
        String enfermedadesBase,
        String enfermedadesOtros,
        ConsumoNivel consumoAlcohol,
        ConsumoTabaco consumoTabaco,
        ConsumoNivel consumoDrogas,
        ActividadFisica actividadFisica,
        String operacionesPrevias,
        String medicamentosRegulares,
        String otrosAntecedentes,
        OffsetDateTime updatedAt
) {
}
