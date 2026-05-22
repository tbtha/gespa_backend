package com.tbtha.gespa_backend.dtos;

public record SlotDisponibleResponse(
    String startsAt,   // ISO OffsetDateTime string
    String endsAt,
    String modalidad,
    Long profesionalId,
    String profesionalNombre,
    String profesionalEspecialidad
) {}
