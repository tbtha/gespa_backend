package com.tbtha.gespa_backend.dtos;

public record ProfesionalResponse(
        Long id,
        String email,
        String displayName,
        String specialty,
        String licenseNumber,
        String phone,
        String institucion,
        String descripcion
) {
}
