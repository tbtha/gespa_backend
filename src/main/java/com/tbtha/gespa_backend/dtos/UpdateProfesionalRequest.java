package com.tbtha.gespa_backend.dtos;

import jakarta.validation.constraints.NotBlank;

public record UpdateProfesionalRequest(
        @NotBlank String displayName,
        @NotBlank String specialty,
        String licenseNumber,
        String phone,
        String address,
        String institucion,
        String descripcion
) {
}
