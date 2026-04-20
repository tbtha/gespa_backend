package com.tbtha.gespa_backend.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateProfesionalRequest(
        @Email @NotBlank String email,
        @NotBlank @Size(min = 6, max = 100) String password,
        @NotBlank String displayName,
        String licenseNumber,
        @NotBlank String specialty,
        String phone,
        String address,
        String institucion,
        String descripcion
) {
}
