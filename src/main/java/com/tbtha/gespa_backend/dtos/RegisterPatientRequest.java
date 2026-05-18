package com.tbtha.gespa_backend.dtos;

import com.tbtha.gespa_backend.validation.ValidRut;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterPatientRequest(
        @Email @NotBlank String email,
        @Size(min = 8, max = 128) String password,
        @NotBlank String displayName,
        @NotBlank @ValidRut String rut
) {
}
