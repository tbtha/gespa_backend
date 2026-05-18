package com.tbtha.gespa_backend.dtos;

public record ProfesionalResponse(
        Long id,
        String email,
        String displayName,
        String rut,
        String specialty,
        String phone,
        String address,
        String institucion,
        String descripcion
) {
}
