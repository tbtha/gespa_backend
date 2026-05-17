package com.tbtha.gespa_backend.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CreatePatientInvitationRequest(
        @Email @NotBlank String email,
        @NotBlank String displayName,
        @NotBlank String rut,
        Long professionalId
) {
}
