package com.tbtha.gespa_backend.dtos;

import com.tbtha.gespa_backend.validation.ValidRut;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record AdminCreateProfessionalInvitationRequest(
        @Email @NotBlank String email,
        @NotBlank String displayName,
        @NotBlank @ValidRut String rut,
        String specialty,
        String phone,
        String address,
        String institucion,
        String descripcion
) {
}
