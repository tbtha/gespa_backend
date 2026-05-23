package com.tbtha.gespa_backend.dtos;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateHorarioRequest(
    @NotNull @Min(1) @Max(7) Integer diaSemana,
    @NotBlank String horaInicio, // "HH:mm"
    @NotBlank String horaFin,    // "HH:mm"
    @Min(10) @Max(240) Integer duracionMinutos,
    @NotBlank String modalidad,   // PRESENCIAL | ONLINE
    String direccionAtencion
) {}
