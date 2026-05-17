package com.tbtha.gespa_backend.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record AdminCreateProfessionalInvitationRequest(
        @Email @NotBlank String email,
        @NotBlank String displayName,
        @NotBlank String rut,
        String specialty,
        String licenseNumber,
        String phone,
        String address,
        String institucion,
        String descripcion
) {
}
