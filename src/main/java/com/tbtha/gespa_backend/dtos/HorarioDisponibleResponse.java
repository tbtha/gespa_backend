package com.tbtha.gespa_backend.dtos;

public record HorarioDisponibleResponse(
    Long id,
    Long profesionalId,
    int diaSemana,
    String horaInicio,
    String horaFin,
    int duracionMinutos,
    String modalidad,
    String direccionAtencion,
    boolean active
) {}
